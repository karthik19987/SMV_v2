package com.shopkeeper.pro.data.database

import androidx.room.TypeConverter
import com.shopkeeper.pro.data.entity.ExpenseCategory
import com.shopkeeper.pro.data.entity.ItemCategory
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromItemCategory(category: ItemCategory): String {
        return category.name
    }

    @TypeConverter
    fun toItemCategory(category: String): ItemCategory {
        return ItemCategory.valueOf(category)
    }

    @TypeConverter
    fun fromExpenseCategory(category: ExpenseCategory): String {
        return category.name
    }

    @TypeConverter
    fun toExpenseCategory(category: String): ExpenseCategory {
        return ExpenseCategory.valueOf(category)
    }
}