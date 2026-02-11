package com.shopkeeper.pro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val username: String,
    val displayName: String,
    val role: String = "user", // admin, user
    val password: String = "1234", // Default password
    val createdAt: Date,
    val isActive: Boolean = true
)

enum class UserRole {
    ADMIN, USER
}