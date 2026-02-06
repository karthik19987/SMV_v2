package com.shopkeeper.pro.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.shopkeeper.pro.data.dao.*
import com.shopkeeper.pro.data.entity.*

@Database(
    entities = [User::class, Item::class, Sale::class, Expense::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ShopKeeperDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ShopKeeperDatabase? = null

        fun getDatabase(context: Context): ShopKeeperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopKeeperDatabase::class.java,
                    "shopkeeper_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}