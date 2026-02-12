package com.shopkeeper.pro.data.firebase.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// Firestore User model
data class FirestoreUser(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "user", // admin, user
    val storeIds: List<String> = emptyList(),
    val isActive: Boolean = true,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

// Firestore Item/Product model
data class FirestoreItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val category: String = "PRODUCT",
    val pricePerKg: Double? = null,
    val unit: String = "kg",
    val storeId: String = "",
    val isActive: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

// Firestore Sale model
data class FirestoreSale(
    @DocumentId
    val id: String = "",
    val items: List<SaleItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val customerName: String? = null,
    val customerPhone: String? = null,
    val userId: String = "",
    val storeId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val syncStatus: String = "synced" // synced, pending, failed
)

// Nested sale item (not a separate collection)
data class SaleItem(
    val itemId: String = "",
    val itemName: String = "",
    val quantity: Double = 0.0,
    val pricePerUnit: Double = 0.0,
    val total: Double = 0.0
)

// Firestore Expense model
data class FirestoreExpense(
    @DocumentId
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "OTHER",
    val imageUrl: String? = null,
    val userId: String = "",
    val storeId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val syncStatus: String = "synced"
)

// Sync Queue item for offline operations
data class SyncQueueItem(
    @DocumentId
    val id: String = "",
    val collection: String = "",
    val documentId: String = "",
    val operation: String = "", // create, update, delete
    val data: Map<String, Any> = emptyMap(),
    val retryCount: Int = 0,
    val storeId: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)