package com.shopkeeper.pro.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShopKeeperDatabase.getDatabase(application)
    private val saleDao = database.saleDao()
    private val expenseDao = database.expenseDao()
    private val itemDao = database.itemDao()

    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                val today = Date()
                val todaySales = saleDao.getTotalSalesForDate(today) ?: 0.0
                val todayExpenses = expenseDao.getTotalExpensesForDate(today) ?: 0.0
                
                // Count active items
                var totalItems = 0
                itemDao.getAllActiveItems().collect { items ->
                    totalItems = items.size
                    
                    _dashboardData.value = DashboardData(
                        todaySales = todaySales,
                        todayExpenses = todayExpenses,
                        totalItems = totalItems
                    )
                }
            } catch (e: Exception) {
                // Handle error
                _dashboardData.value = DashboardData()
            }
        }
    }
}

data class DashboardData(
    val todaySales: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val totalItems: Int = 0
)