package com.shopkeeper.pro.ui.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.entity.Item
import com.shopkeeper.pro.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class ProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShopKeeperDatabase.getDatabase(application)
    private val itemDao = database.itemDao()
    private val itemRepository = ItemRepository(itemDao)

    private val _allProducts = MutableStateFlow<List<Item>>(emptyList())
    val allProducts: StateFlow<List<Item>> = _allProducts

    init {
        loadAllProducts()
    }

    private fun loadAllProducts() {
        viewModelScope.launch {
            // Get all items including inactive ones for management
            database.itemDao().getAllActiveItems().collect { activeItems ->
                // For now just show active items, but we could modify to show all
                _allProducts.value = activeItems
            }
        }
    }

    fun addProduct(product: Item) {
        viewModelScope.launch {
            try {
                itemDao.insertItem(product)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProduct(product: Item) {
        viewModelScope.launch {
            try {
                itemDao.updateItem(product)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteProduct(product: Item) {
        viewModelScope.launch {
            try {
                itemDao.deleteItem(product.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleProductActive(product: Item) {
        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(isActive = !product.isActive)
                itemDao.updateItem(updatedProduct)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addDefaultItems() {
        viewModelScope.launch {
            itemRepository.insertDefaultItems("user_1")
        }
    }
}