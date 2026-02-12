package com.shopkeeper.pro.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.shopkeeper.pro.data.database.ShopKeeperDatabase
import com.shopkeeper.pro.data.firebase.FirebaseSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "SyncWorker"
    private val database = ShopKeeperDatabase.getDatabase(context)
    private val syncRepository = FirebaseSyncRepository()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting sync...")

                // Sync items/products
                syncItems()

                // Sync sales
                syncSales()

                // Sync expenses
                syncExpenses()

                Log.d(TAG, "Sync completed successfully")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                if (runAttemptCount < MAX_RETRY_COUNT) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    private suspend fun syncItems() {
        try {
            val localItems = database.itemDao().getAllActiveItems().first()
            localItems.forEach { item ->
                val result = syncRepository.syncItemToFirestore(item)
                if (result.isFailure) {
                    Log.e(TAG, "Failed to sync item: ${item.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing items", e)
            throw e
        }
    }

    private suspend fun syncSales() {
        try {
            // Sync only unsynced sales
            val unsyncedSales = database.saleDao().getAllSales().first()
                .filter { it.syncStatus != "synced" }

            if (unsyncedSales.isNotEmpty()) {
                val result = syncRepository.batchSyncSales(unsyncedSales)
                if (result.isSuccess) {
                    // Mark sales as synced
                    unsyncedSales.forEach { sale ->
                        database.saleDao().updateSale(sale.copy(syncStatus = "synced"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing sales", e)
            throw e
        }
    }

    private suspend fun syncExpenses() {
        try {
            val localExpenses = database.expenseDao().getAllExpenses().first()
                .filter { it.syncStatus != "synced" }

            localExpenses.forEach { expense ->
                val result = syncRepository.syncExpenseToFirestore(expense)
                if (result.isSuccess) {
                    database.expenseDao().updateExpense(expense.copy(syncStatus = "synced"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing expenses", e)
            throw e
        }
    }

    companion object {
        private const val MAX_RETRY_COUNT = 3
        const val SYNC_WORK_NAME = "periodic_sync"
        const val IMMEDIATE_SYNC_WORK = "immediate_sync"

        // Schedule periodic sync (every 30 minutes when connected to network)
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                30, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    syncRequest
                )

            Log.d("SyncWorker", "Periodic sync scheduled")
        }

        // Trigger immediate sync
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_SYNC_WORK,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

            Log.d("SyncWorker", "Immediate sync triggered")
        }

        // Cancel all sync work
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(SYNC_WORK_NAME)
                cancelUniqueWork(IMMEDIATE_SYNC_WORK)
            }
        }
    }
}