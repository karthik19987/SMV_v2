package com.shopkeeper.pro.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.User
import com.shopkeeper.pro.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
                    _loginState.value = LoginState.Success(user)
                } else {
                    _loginState.value = LoginState.Error("Invalid username or password")
                }
            } catch (e: Exception) {
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
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}