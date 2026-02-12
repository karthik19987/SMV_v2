package com.shopkeeper.pro.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.User
import com.shopkeeper.pro.data.firebase.FirebaseAuthManager
import com.shopkeeper.pro.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirebaseLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = FirebaseAuthManager()
    private val database = ShopKeeperDatabase.getDatabase(application)
    private val userDao = database.userDao()
    private val itemRepository = ItemRepository(database.itemDao())

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    init {
        // Check if user is already logged in with Firebase
        if (authManager.isUserLoggedIn()) {
            // Auto-login
            viewModelScope.launch {
                authManager.getCurrentUser()?.let { firebaseUser ->
                    try {
                        val user = userDao.getUserById(firebaseUser.uid)
                        if (user != null) {
                            UserPreferences.setCurrentUser(getApplication(), user)
                            _loginState.value = LoginState.Success(user)
                        }
                    } catch (e: Exception) {
                        // User not in local DB, sign out
                        authManager.signOut()
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val result = authManager.signIn(email, password)
                if (result.isSuccess) {
                    val user = result.getOrThrow()

                    // Save user to local database
                    userDao.insertUser(user)

                    // Set current user in preferences
                    UserPreferences.setCurrentUser(getApplication(), user)

                    _loginState.value = LoginState.Success(user)
                } else {
                    _loginState.value = LoginState.Error(
                        result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun createAccount(
        email: String,
        password: String,
        username: String,
        displayName: String,
        role: String = "user"
    ) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val result = authManager.createAccount(
                    email = email,
                    password = password,
                    username = username,
                    displayName = displayName,
                    role = role
                )

                if (result.isSuccess) {
                    val user = result.getOrThrow()

                    // Save user to local database
                    userDao.insertUser(user)

                    // Create default items for the shop
                    itemRepository.insertDefaultItems(user.id)

                    // Set current user in preferences
                    UserPreferences.setCurrentUser(getApplication(), user)

                    _loginState.value = LoginState.Success(user)
                } else {
                    _loginState.value = LoginState.Error(
                        result.exceptionOrNull()?.message ?: "Account creation failed"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Failed to create account: ${e.message}")
            }
        }
    }

    fun createDemoUsers() {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val result = authManager.createDemoUsers()
                if (result.isSuccess) {
                    // Login as admin after creating demo users
                    login("admin@shopkeeperpro.com", "admin123")
                } else {
                    _loginState.value = LoginState.Error(
                        "Failed to create demo users. They might already exist."
                    )
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Failed to create demo users: ${e.message}")
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            try {
                val result = authManager.resetPassword(email)
                if (result.isSuccess) {
                    _loginState.value = LoginState.Error("Password reset email sent to $email")
                } else {
                    _loginState.value = LoginState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error: ${e.message}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            UserPreferences.clearUser(getApplication())
            _loginState.value = LoginState.Idle
        }
    }
}