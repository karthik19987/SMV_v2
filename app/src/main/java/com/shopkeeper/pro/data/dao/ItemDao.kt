package com.shopkeeper.pro.data.dao

import androidx.room.*
import com.shopkeeper.pro.data.entity.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY name")
    fun getAllActiveItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Query("UPDATE items SET isActive = 0 WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("SELECT * FROM items WHERE name LIKE '%' || :searchQuery || '%' AND isActive = 1")
    fun searchItems(searchQuery: String): Flow<List<Item>>
}