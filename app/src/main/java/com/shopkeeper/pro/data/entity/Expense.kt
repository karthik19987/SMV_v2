package com.shopkeeper.pro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String,
    val userId: String,
    val category: ExpenseCategory,
    val description: String,
    val amount: Double,
    val createdAt: Date
)

enum class ExpenseCategory {
    PURCHASE, DAILY_WAGES, BILLS, RENT, ELECTRICITY, TRANSPORT, OTHER
}