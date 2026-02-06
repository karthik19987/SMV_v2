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
                
                val user = userDao.getUserByUsername(username)
                if (user != null && user.isActive) {
                    // In a real app, you'd verify the password here
                    // For demo purposes, any password works
                    UserPreferences.setCurrentUser(getApplication(), user)
                    _loginState.value = LoginState.Success(user)
                } else {
                    _loginState.value = LoginState.Error("Invalid username or user is inactive")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun createDemoUser() {
        viewModelScope.launch {
            try {
                val demoUser = User(
                    id = "demo_user_${System.currentTimeMillis()}",
                    username = "admin",
                    displayName = "Shop Admin",
                    role = "owner",
                    createdAt = Date(),
                    isActive = true
                )
                userDao.insertUser(demoUser)
                
                // Create default items for the shop
                itemRepository.insertDefaultItems(demoUser.id)
                
                UserPreferences.setCurrentUser(getApplication(), demoUser)
                _loginState.value = LoginState.Success(demoUser)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Failed to create demo user: ${e.message}")
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