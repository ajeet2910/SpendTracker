package com.transactiontracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["messageHash"], unique = true),
        Index(value = ["occurredAt"]),
        Index(value = ["trackedAccountId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackedAccountId: Long,
    val bankName: String,
    val accountType: String,
    val lastDigits: String,
    val amount: Double,
    val direction: String,
    val paymentCategory: String,
    val merchant: String?,
    val smsSender: String,
    val occurredAt: Long,
    val receivedAt: Long,
    val messageHash: String,
    val rawMessage: String? = null
)
