package com.transactiontracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracked_accounts",
    indices = [Index(value = ["bankName", "lastDigits", "type"], unique = true)]
)
data class TrackedAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val type: String,
    val lastDigits: String,
    val nickname: String,
    val senderHints: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
