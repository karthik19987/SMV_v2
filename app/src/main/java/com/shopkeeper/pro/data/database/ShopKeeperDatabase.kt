package com.shopkeeper.pro.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.shopkeeper.pro.data.dao.*
import com.shopkeeper.pro.data.entity.*

@Database(
    entities = [User::class, Item::class, Sale::class, Expense::class],
    version = 4,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN imageUri TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add password column to users table with default value
                database.execSQL("ALTER TABLE users ADD COLUMN password TEXT NOT NULL DEFAULT '1234'")
                // Update role values from cashier to user
                database.execSQL("UPDATE users SET role = 'user' WHERE role = 'cashier'")
                database.execSQL("UPDATE users SET role = 'admin' WHERE role = 'owner'")
                // Set specific password for admin user
                database.execSQL("UPDATE users SET password = 'admin123' WHERE username = 'admin' AND role = 'admin'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add sync status to sales and expenses
                database.execSQL("ALTER TABLE sales ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'")
                database.execSQL("ALTER TABLE expenses ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'")

                // Add customer info to sales
                database.execSQL("ALTER TABLE sales ADD COLUMN customerName TEXT")
                database.execSQL("ALTER TABLE sales ADD COLUMN customerPhone TEXT")
            }
        }

        fun getDatabase(context: Context): ShopKeeperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopKeeperDatabase::class.java,
                    "shopkeeper_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}