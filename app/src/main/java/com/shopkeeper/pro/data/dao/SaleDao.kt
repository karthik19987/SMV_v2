package com.shopkeeper.pro.data.dao

import androidx.room.*
import com.shopkeeper.pro.data.entity.Sale
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE DATE(createdAt/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch') ORDER BY createdAt DESC")
    fun getSalesByDate(date: Date): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSalesByUser(userId: String): Flow<List<Sale>>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE DATE(createdAt/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    suspend fun getTotalSalesForDate(date: Date): Double?

    @Insert
    suspend fun insertSale(sale: Sale)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSale(id: String)
}