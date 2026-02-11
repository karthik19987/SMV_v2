package com.shopkeeper.pro.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import kotlinx.coroutines.launch
import java.util.*

class SalesFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShopKeeperDatabase.getDatabase(application)
    private val saleDao = database.saleDao()

    private val _todaySales = MutableLiveData<Double>()
    val todaySales: LiveData<Double> = _todaySales

    init {
        loadTodaySales()
    }

    fun loadTodaySales() {
        viewModelScope.launch {
            try {
                val today = Date()
                val total = saleDao.getTotalSalesForDate(today) ?: 0.0
                _todaySales.postValue(total)
            } catch (e: Exception) {
                e.printStackTrace()
                _todaySales.postValue(0.0)
            }
        }
    }
}