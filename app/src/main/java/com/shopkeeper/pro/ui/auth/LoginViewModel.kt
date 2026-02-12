package com.shopkeeper.pro.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.User
import com.shopkeeper.pro.data.repository.ItemRepository
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = ShopKeeperDatabase.getDatabase(application)
    private val userDao = database.userDao()
    private val itemRepository = ItemRepository(database.itemDao())
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading

                val user = userDao.getUserByCredentials(username, password)
                if (user != null) {
                    UserPreferences.setCurrentUser(getApplication(), user)
                    Log.d("LoginViewModel", "Login successful for user: ${user.username}, role: ${user.role}")
                    // Small delay to ensure preferences are saved
                    kotlinx.coroutines.delay(100)
                    _loginState.value = LoginState.Success(user)
                } else {
                    // Try to get user by username to provide better error
                    val userByName = userDao.getUserByUsername(username)
                    if (userByName != null) {
                        Log.d("LoginViewModel", "User found but wrong password. Expected: ${userByName.password}")
                        _loginState.value = LoginState.Error("Wrong password for user: $username")
                    } else {
                        _loginState.value = LoginState.Error("User not found: $username")
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login error", e)
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun createDemoUser() {
        viewModelScope.launch {
            try {
                // Create admin user
                val adminUser = User(
                    id = "admin_${System.currentTimeMillis()}",
                    username = "admin",
                    displayName = "Shop Admin",
                    role = "admin",
                    password = "admin123", // Default admin password
                    createdAt = Date(),
                    isActive = true
                )
                userDao.insertUser(adminUser)

                // Create regular user
                val regularUser = User(
                    id = "user_${System.currentTimeMillis()}",
                    username = "user1",
                    displayName = "Shop User",
                    role = "user",
                    password = "1234", // Default user password
                    createdAt = Date(),
                    isActive = true
                )
                userDao.insertUser(regularUser)

                // Create default items for the shop
                itemRepository.insertDefaultItems(adminUser.id)

                // Auto-login as admin
                UserPreferences.setCurrentUser(getApplication(), adminUser)
                _loginState.value = LoginState.Success(adminUser)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Failed to create demo users: ${e.message}")
            }
        }
    }

    fun debugGetAllUsers() {
        viewModelScope.launch {
            try {
                val allUsers = userDao.getAllActiveUsers().first()
                Log.d("LoginViewModel", "Total active users: ${allUsers.size}")
                allUsers.forEach { user ->
                    Log.d("LoginViewModel", "User: ${user.username}, Password: ${user.password}, Role: ${user.role}")
                }

                if (allUsers.isEmpty()) {
                    Log.d("LoginViewModel", "No users found. Click 'Create Demo User' button.")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error getting users", e)
            }
        }
    }

    fun resetAdminPassword() {
        viewModelScope.launch {
            try {
                // First try to find existing admin user
                val adminUser = userDao.getUserByUsername("admin")
                if (adminUser != null) {
                    // Update the existing admin user's password
                    val updatedUser = adminUser.copy(
                        password = "admin123",
                        role = "admin" // Ensure role is admin
                    )
                    userDao.insertUser(updatedUser) // This will replace due to OnConflictStrategy.REPLACE
                    Log.d("LoginViewModel", "Admin password reset successfully")
                } else {
                    // Create new admin user if doesn't exist
                    val newAdmin = User(
                        id = "admin_default",
                        username = "admin",
                        displayName = "Shop Admin",
                        role = "admin",
                        password = "admin123",
                        createdAt = Date(),
                        isActive = true
                    )
                    userDao.insertUser(newAdmin)
                    Log.d("LoginViewModel", "Admin user created with password 'admin123'")
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error resetting admin password", e)
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}