package com.shopkeeper.pro.data.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.shopkeeper.pro.data.entity.*
import com.shopkeeper.pro.data.firebase.models.*
import com.shopkeeper.pro.data.firebase.models.SaleItem as FirestoreSaleItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseSyncRepository {
    private val firestore = FirebaseConfig.firestore
    private val TAG = "FirebaseSync"

    // Store listeners to manage them
    private val listeners = mutableMapOf<String, ListenerRegistration>()

    // ===== USER SYNC =====
    suspend fun syncUserToFirestore(user: User): Result<Unit> {
        return try {
            val firestoreUser = user.toFirestoreModel()
            firestore.collection(FirebaseConfig.Collections.USERS)
                .document(user.id)
                .set(firestoreUser)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user to Firestore", e)
            Result.failure(e)
        }
    }

    fun observeFirestoreUsers(): Flow<List<FirestoreUser>> = callbackFlow {
        val listenerRegistration = firestore.collection(FirebaseConfig.Collections.USERS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing users", error)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val users = it.documents.mapNotNull { doc ->
                        doc.toObject(FirestoreUser::class.java)
                    }
                    trySend(users)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // ===== ITEM/PRODUCT SYNC =====
    suspend fun syncItemToFirestore(item: Item): Result<Unit> {
        return try {
            val storeId = FirebaseConfig.getCurrentStoreId()
            val firestoreItem = item.toFirestoreModel(storeId)

            firestore.collection(FirebaseConfig.Collections.STORES)
                .document(storeId)
                .collection(FirebaseConfig.Collections.ITEMS)
                .document(item.id)
                .set(firestoreItem)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing item to Firestore", e)
            Result.failure(e)
        }
    }

    fun observeFirestoreItems(storeId: String): Flow<List<FirestoreItem>> = callbackFlow {
        val listenerRegistration = firestore.collection(FirebaseConfig.Collections.STORES)
            .document(storeId)
            .collection(FirebaseConfig.Collections.ITEMS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing items", error)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val items = it.documents.mapNotNull { doc ->
                        doc.toObject(FirestoreItem::class.java)
                    }
                    trySend(items)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // ===== SALE SYNC =====
    suspend fun syncSaleToFirestore(sale: Sale): Result<Unit> {
        return try {
            val storeId = FirebaseConfig.getCurrentStoreId()
            val firestoreSale = sale.toFirestoreModel(storeId)

            firestore.collection(FirebaseConfig.Collections.STORES)
                .document(storeId)
                .collection(FirebaseConfig.Collections.SALES)
                .document(sale.id)
                .set(firestoreSale)
                .await()

            // Update store daily totals
            updateStoreDailyTotals(storeId, sale.totalAmount)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing sale to Firestore", e)
            Result.failure(e)
        }
    }

    // ===== EXPENSE SYNC =====
    suspend fun syncExpenseToFirestore(expense: Expense): Result<Unit> {
        return try {
            val storeId = FirebaseConfig.getCurrentStoreId()
            val firestoreExpense = expense.toFirestoreModel(storeId)

            firestore.collection(FirebaseConfig.Collections.STORES)
                .document(storeId)
                .collection(FirebaseConfig.Collections.EXPENSES)
                .document(expense.id)
                .set(firestoreExpense)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing expense to Firestore", e)
            Result.failure(e)
        }
    }

    // ===== BATCH OPERATIONS =====
    suspend fun batchSyncSales(sales: List<Sale>): Result<Unit> {
        return try {
            val storeId = FirebaseConfig.getCurrentStoreId()
            val batch = firestore.batch()

            sales.forEach { sale ->
                val ref = firestore.collection(FirebaseConfig.Collections.STORES)
                    .document(storeId)
                    .collection(FirebaseConfig.Collections.SALES)
                    .document(sale.id)

                batch.set(ref, sale.toFirestoreModel(storeId))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error batch syncing sales", e)
            Result.failure(e)
        }
    }

    // ===== HELPER FUNCTIONS =====
    private suspend fun updateStoreDailyTotals(storeId: String, saleAmount: Double) {
        val today = Date()
        val dateStr = android.text.format.DateFormat.format("yyyy-MM-dd", today).toString()

        firestore.collection(FirebaseConfig.Collections.STORES)
            .document(storeId)
            .collection("daily_totals")
            .document(dateStr)
            .update(
                "totalSales", FieldValue.increment(saleAmount),
                "saleCount", FieldValue.increment(1),
                "lastUpdated", FieldValue.serverTimestamp()
            )
            .addOnFailureListener {
                // If document doesn't exist, create it
                firestore.collection(FirebaseConfig.Collections.STORES)
                    .document(storeId)
                    .collection("daily_totals")
                    .document(dateStr)
                    .set(mapOf(
                        "totalSales" to saleAmount,
                        "saleCount" to 1,
                        "totalExpenses" to 0.0,
                        "expenseCount" to 0,
                        "date" to dateStr,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    ))
            }
    }

    // Cleanup listeners
    fun removeAllListeners() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}

// Extension functions to convert between Room entities and Firestore models
private fun User.toFirestoreModel(): FirestoreUser {
    return FirestoreUser(
        id = this.id,
        username = this.username,
        displayName = this.displayName,
        role = this.role,
        isActive = this.isActive,
        storeIds = listOf(FirebaseConfig.getCurrentStoreId())
    )
}

private fun Item.toFirestoreModel(storeId: String): FirestoreItem {
    return FirestoreItem(
        id = this.id,
        name = this.name,
        category = this.category.name,
        pricePerKg = this.pricePerKg,
        unit = this.unit,
        storeId = storeId,
        isActive = this.isActive,
        createdBy = this.createdBy
    )
}

private fun Sale.toFirestoreModel(storeId: String): FirestoreSale {
    return FirestoreSale(
        id = this.id,
        items = this.items.split(";").filter { it.isNotBlank() }.map { itemStr ->
            val parts = itemStr.split(",")
            if (parts.size >= 5) {
                FirestoreSaleItem(
                    itemId = "", // We don't have item ID in the old format
                    itemName = parts[0],
                    quantity = parts[1].toDoubleOrNull() ?: 0.0,
                    pricePerUnit = parts[2].toDoubleOrNull() ?: 0.0,
                    total = parts[3].toDoubleOrNull() ?: 0.0
                )
            } else {
                FirestoreSaleItem()
            }
        },
        totalAmount = this.totalAmount,
        paymentMethod = this.paymentMethod,
        customerName = this.customerName,
        customerPhone = this.customerPhone,
        userId = this.userId,
        storeId = storeId
    )
}

private fun Expense.toFirestoreModel(storeId: String): FirestoreExpense {
    return FirestoreExpense(
        id = this.id,
        description = this.description,
        amount = this.amount,
        category = this.category.name,
        imageUrl = this.imageUri,
        userId = this.userId,
        storeId = storeId
    )
}