package com.shopkeeper.pro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey
    val id: String,
    val userId: String,
    val totalAmount: Double,
    val items: String, // JSON string of SaleItem list
    val createdAt: Date,
    val paymentMethod: String = "cash",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val syncStatus: String = "pending" // pending, synced, failed
)

data class SaleItem(
    val itemId: String,
    val itemName: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val totalPrice: Double
)