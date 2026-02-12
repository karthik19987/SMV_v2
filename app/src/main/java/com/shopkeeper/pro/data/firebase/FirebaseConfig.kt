package com.shopkeeper.pro.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirebaseConfig {

    // Firebase instances
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            // Enable offline persistence
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
        }
    }

    // Firestore collection names
    object Collections {
        const val USERS = "users"
        const val STORES = "stores"
        const val ITEMS = "items"
        const val SALES = "sales"
        const val EXPENSES = "expenses"
        const val SYNC_QUEUE = "sync_queue"
    }

    // Get current store ID (for multi-store support in future)
    fun getCurrentStoreId(): String {
        // For now, return a default store ID
        // In future, this can be selected by the user
        return "store_default"
    }

    // Get current user ID from Firebase Auth
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}