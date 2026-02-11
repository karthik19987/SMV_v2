package com.shopkeeper.pro.ui.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Sale
import com.shopkeeper.pro.data.entity.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShopKeeperDatabase.getDatabase(application)
    private val saleDao = database.saleDao()
    private val itemDao = database.itemDao()

    private val _allSales = MutableStateFlow<List<Sale>>(emptyList())
    val allSales: StateFlow<List<Sale>> = _allSales

    private val _activeProducts = MutableStateFlow<List<Item>>(emptyList())
    val activeProducts: StateFlow<List<Item>> = _activeProducts

    init {
        loadAllSales()
        loadActiveProducts()
    }

    private fun loadAllSales() {
        viewModelScope.launch {
            saleDao.getAllSales().collect { sales ->
                _allSales.value = sales
            }
        }
    }

    private fun loadActiveProducts() {
        viewModelScope.launch {
            itemDao.getAllActiveItems().collect { products ->
                // Log for debugging
                android.util.Log.d("SalesViewModel", "Loaded ${products.size} active products from database")
                products.forEach { product ->
                    android.util.Log.d("SalesViewModel", "Product: ${product.name} (${product.id}) - Active: ${product.isActive} - Category: ${product.category}")
                }
                _activeProducts.value = products
            }
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            try {
                saleDao.deleteSale(sale)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAllSales() {
        viewModelScope.launch {
            try {
                saleDao.deleteAllSales()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSale(sale: Sale) {
        viewModelScope.launch {
            try {
                saleDao.updateSale(sale)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshProducts() {
        android.util.Log.d("SalesViewModel", "Manually refreshing products")
        viewModelScope.launch {
            try {
                val products = itemDao.getAllActiveItemsOnce()
                android.util.Log.d("SalesViewModel", "Direct query found ${products.size} products")
                products.forEach { product ->
                    android.util.Log.d("SalesViewModel", "Direct query - Product: ${product.name}")
                }
                _activeProducts.value = products
            } catch (e: Exception) {
                android.util.Log.e("SalesViewModel", "Error refreshing products", e)
            }
        }
    }
}