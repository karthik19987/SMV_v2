package com.shopkeeper.pro.data.repository

import com.shopkeeper.pro.data.dao.ItemDao
import com.shopkeeper.pro.data.entity.Item
import com.shopkeeper.pro.data.entity.ItemCategory
import kotlinx.coroutines.flow.Flow
import java.util.*

class ItemRepository(private val itemDao: ItemDao) {

    fun getAllItems(): Flow<List<Item>> = itemDao.getAllActiveItems()

    suspend fun getItemById(id: String): Item? = itemDao.getItemById(id)

    suspend fun insertItem(item: Item) = itemDao.insertItem(item)

    suspend fun updateItem(item: Item) = itemDao.updateItem(item)

    suspend fun deleteItem(id: String) = itemDao.deleteItem(id)

    fun searchItems(query: String): Flow<List<Item>> = itemDao.searchItems(query)

    suspend fun insertDefaultItems(userId: String) {
        val defaultItems = listOf(
            Item(
                id = "item_${System.currentTimeMillis()}_1",
                name = "Podipp",
                category = ItemCategory.PRODUCT,
                pricePerKg = 50.0,
                unit = "kg",
                createdAt = Date(),
                createdBy = userId
            ),
            Item(
                id = "item_${System.currentTimeMillis()}_2",
                name = "Chilly",
                category = ItemCategory.PRODUCT,
                pricePerKg = 80.0,
                unit = "kg",
                createdAt = Date(),
                createdBy = userId
            ),
            Item(
                id = "item_${System.currentTimeMillis()}_3",
                name = "Malli",
                category = ItemCategory.PRODUCT,
                pricePerKg = 120.0,
                unit = "kg",
                createdAt = Date(),
                createdBy = userId
            ),
            Item(
                id = "item_${System.currentTimeMillis()}_4",
                name = "Unda",
                category = ItemCategory.PRODUCT,
                pricePerKg = null,
                unit = "pc",
                createdAt = Date(),
                createdBy = userId
            ),
            Item(
                id = "item_${System.currentTimeMillis()}_5",
                name = "Repair Service",
                category = ItemCategory.SERVICE,
                pricePerKg = null,
                unit = "service",
                createdAt = Date(),
                createdBy = userId
            )
        )
        
        defaultItems.forEach { item ->
            insertItem(item)
        }
    }
}