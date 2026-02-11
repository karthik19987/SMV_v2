package com.shopkeeper.pro.data.dao

import androidx.room.*
import com.shopkeeper.pro.data.entity.Expense
import com.shopkeeper.pro.data.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE DATE(createdAt/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch') ORDER BY createdAt DESC")
    fun getExpensesByDate(date: Date): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY createdAt DESC")
    fun getExpensesByCategory(category: ExpenseCategory): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE DATE(createdAt/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    suspend fun getTotalExpensesForDate(date: Date): Double?

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}