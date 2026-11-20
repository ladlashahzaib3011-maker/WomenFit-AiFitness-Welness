package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Secure User Account Entity
 *
 * Stores core credentials, password hash with unique salt, and account status.
 * Notice: Plain-text passwords are NEVER stored in this database.
 */
@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccount(
    @PrimaryKey val userId: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val accountStatus: String = "ACTIVE", // "ACTIVE", "VERIFIED", "SUSPENDED"
    val lastLoginTimestamp: Long = 0L,
    val resetCode: String? = null,
    val resetCodeExpiry: Long = 0L
)
