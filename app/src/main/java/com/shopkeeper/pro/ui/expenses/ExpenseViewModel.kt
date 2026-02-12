package com.shopkeeper.pro.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Expense
import com.shopkeeper.pro.data.entity.ExpenseCategory
import com.shopkeeper.pro.data.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShopKeeperDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()

    private val _todayExpenses = MutableLiveData<Double>()
    val todayExpenses: LiveData<Double> = _todayExpenses

    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _allExpenses

    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    init {
        loadTodayExpenses()
        loadAllExpenses()
    }

    fun loadTodayExpenses() {
        viewModelScope.launch {
            try {
                val today = Date()
                val total = expenseDao.getTotalExpensesForDate(today) ?: 0.0
                _todayExpenses.postValue(total)
            } catch (e: Exception) {
                e.printStackTrace()
                _todayExpenses.postValue(0.0)
            }
        }
    }

    private fun loadAllExpenses() {
        viewModelScope.launch {
            expenseDao.getAllExpenses().collect { expenses ->
                _allExpenses.value = expenses
            }
        }
    }

    fun addExpense(description: String, amount: Double, category: ExpenseCategory, imageUri: String? = null) {
        viewModelScope.launch {
            try {
                _saveState.value = SaveState.Loading

                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    userId = "user_1", // In production, get from UserPreferences
                    description = description,
                    amount = amount,
                    category = category,
                    createdAt = Date(),
                    imageUri = imageUri
                )

                expenseDao.insertExpense(expense)
                loadTodayExpenses() // Refresh today's total
                _saveState.value = SaveState.Success

                // Trigger sync to Firebase
                SyncWorker.triggerImmediateSync(getApplication())

            } catch (e: Exception) {
                e.printStackTrace()
                _saveState.value = SaveState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                expenseDao.deleteExpense(expense)
                loadTodayExpenses() // Refresh today's total
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAllExpenses() {
        viewModelScope.launch {
            try {
                expenseDao.deleteAllExpenses()
                loadTodayExpenses() // Refresh today's total
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                expenseDao.updateExpense(expense)
                loadTodayExpenses() // Refresh today's total
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}