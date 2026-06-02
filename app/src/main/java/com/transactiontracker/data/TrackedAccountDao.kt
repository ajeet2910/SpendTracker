package com.transactiontracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAccountDao {
    @Query("SELECT * FROM tracked_accounts ORDER BY createdAt DESC")
    fun observeAccounts(): Flow<List<TrackedAccountEntity>>

    @Query("SELECT * FROM tracked_accounts WHERE enabled = 1")
    suspend fun getEnabledAccounts(): List<TrackedAccountEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsert(account: TrackedAccountEntity): Long

    @Query("DELETE FROM tracked_accounts WHERE id = :id")
    suspend fun delete(id: Long)
}
