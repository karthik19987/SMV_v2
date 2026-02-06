package com.shopkeeper.pro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: ItemCategory,
    val pricePerKg: Double? = null, // For products, price hint per kg
    val unit: String = "pc", // pc, kg, ltr, etc.
    val isActive: Boolean = true,
    val createdAt: Date,
    val createdBy: String
)

enum class ItemCategory {
    PRODUCT, SERVICE
}