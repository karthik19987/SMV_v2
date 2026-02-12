package com.shopkeeper.pro

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.shopkeeper.pro.data.sync.SyncWorker

class ShopKeeperApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("ShopKeeperApp", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("ShopKeeperApp", "Firebase initialization failed", e)
        }

        // Schedule periodic sync
        SyncWorker.schedulePeriodicSync(this)
    }
}