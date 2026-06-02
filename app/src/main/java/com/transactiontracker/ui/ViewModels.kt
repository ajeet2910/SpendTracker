package com.transactiontracker.ui

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transactiontracker.data.AppDatabase
import com.transactiontracker.data.TrackedAccountEntity
import com.transactiontracker.data.TransactionEntity
import com.transactiontracker.security.RawSmsCrypto
import com.transactiontracker.sms.SmsSyncRepository
import com.transactiontracker.sms.SmsSyncResult
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)

    val accounts = database.trackedAccountDao()
        .observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAccount(
        bankName: String,
        type: String,
        lastDigits: String,
        nickname: String,
        senderHints: String
    ) {
        if (bankName.isBlank() || lastDigits.length < 3) return
        viewModelScope.launch {
            database.trackedAccountDao().upsert(
                TrackedAccountEntity(
                    bankName = bankName.trim(),
                    type = type,
                    lastDigits = lastDigits.trim(),
                    nickname = nickname.trim(),
                    senderHints = senderHints.trim()
                )
            )
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            database.trackedAccountDao().delete(id)
        }
    }
}

class SyncSmsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsSyncRepository(application, AppDatabase.get(application))

    var result = MutableStateFlow<SmsSyncResult?>(null)
        private set

    var isSyncing = MutableStateFlow(false)
        private set

    fun hasSmsPermission(): Boolean = repository.hasSmsPermission()

    fun syncInbox() {
        viewModelScope.launch {
            isSyncing.value = true
            result.value = repository.syncInbox(saveRawTransactionSms = true)
            isSyncing.value = false
        }
    }

    fun resetAndSyncInbox() {
        viewModelScope.launch {
            isSyncing.value = true
            result.value = repository.resetAndSyncInbox(saveRawTransactionSms = true)
            isSyncing.value = false
        }
    }

    companion object {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val month = selectedMonth

    val transactions = selectedMonth
        .flatMapLatest { month ->
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            database.transactionDao().observeTransactionsBetween(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }
}

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val rawSmsCrypto = RawSmsCrypto()

    val transactions = database.transactionDao()
        .observeRecentTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rawMessageFor(transaction: TransactionEntity): String? {
        return rawSmsCrypto.decrypt(transaction.rawMessage)
    }
}

class FilterTransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val rawSmsCrypto = RawSmsCrypto()

    val transactions = database.transactionDao()
        .observeAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rawMessageFor(transaction: TransactionEntity): String? {
        return rawSmsCrypto.decrypt(transaction.rawMessage)
    }
}

fun List<TransactionEntity>.debitTotal(): Double {
    return filter { it.direction == "debit" }.sumOf { it.amount }
}

fun List<TransactionEntity>.creditTotal(): Double {
    return filter { it.direction == "credit" }.sumOf { it.amount }
}
