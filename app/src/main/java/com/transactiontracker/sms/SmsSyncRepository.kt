package com.transactiontracker.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.transactiontracker.data.AppDatabase
import com.transactiontracker.data.TransactionEntity
import com.transactiontracker.parser.TransactionSmsParser
import com.transactiontracker.parser.messageHash
import com.transactiontracker.security.RawSmsCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsSyncRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val parser = TransactionSmsParser()
    private val rawSmsCrypto = RawSmsCrypto()

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun syncInbox(saveRawTransactionSms: Boolean = false): SmsSyncResult = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) {
            return@withContext SmsSyncResult(0, 0, 0, 0, 0)
        }

        val accounts = database.trackedAccountDao().getEnabledAccounts()
        if (accounts.isEmpty()) {
            return@withContext SmsSyncResult(0, 0, 0, 0, 0)
        }

        var scanned = 0
        var matchedCandidates = 0
        var saved = 0
        var duplicates = 0

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                scanned += 1
                val sender = cursor.getString(addressIndex).orEmpty()
                val body = cursor.getString(bodyIndex).orEmpty()
                val receivedAt = cursor.getLong(dateIndex)

                val transaction = accounts.firstNotNullOfOrNull { account ->
                    parser.parse(sender, body, account)?.let { parsed ->
                        matchedCandidates += 1
                        TransactionEntity(
                            trackedAccountId = account.id,
                            bankName = account.bankName,
                            accountType = account.type,
                            lastDigits = account.lastDigits,
                            amount = parsed.amount,
                            direction = parsed.direction,
                            paymentCategory = parsed.paymentCategory,
                            merchant = parsed.merchant,
                            smsSender = sender,
                            occurredAt = receivedAt,
                            receivedAt = receivedAt,
                            messageHash = messageHash(sender, body, receivedAt),
                            rawMessage = if (saveRawTransactionSms) rawSmsCrypto.encrypt(body) else null
                        )
                    }
                }

                if (transaction != null) {
                    val id = database.transactionDao().insert(transaction)
                    if (id == -1L) duplicates += 1 else saved += 1
                }
            }
        }

        SmsSyncResult(
            scanned = scanned,
            matchedCandidates = matchedCandidates,
            saved = saved,
            duplicates = duplicates,
            discarded = scanned - matchedCandidates
        )
    }

    suspend fun resetAndSyncInbox(saveRawTransactionSms: Boolean = true): SmsSyncResult = withContext(Dispatchers.IO) {
        database.transactionDao().deleteAll()
        syncInbox(saveRawTransactionSms)
    }
}
