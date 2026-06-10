package com.transactiontracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.transactiontracker.data.AppDatabase
import com.transactiontracker.data.TransactionEntity
import com.transactiontracker.parser.TransactionSmsParser
import com.transactiontracker.parser.messageHash
import com.transactiontracker.security.RawSmsCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = TransactionSmsParser()
    private val rawSmsCrypto = RawSmsCrypto()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                val database = AppDatabase.get(context)
                val accounts = database.trackedAccountDao().getEnabledAccounts()
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                messages.groupBy { it.originatingAddress }.forEach { (senderAddress, smsParts) ->
                    val sender = senderAddress.orEmpty()
                    val body = smsParts.joinToString("") { it.messageBody.orEmpty() }
                    val receivedAt = smsParts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

                    val transaction = accounts.firstNotNullOfOrNull { account ->
                        parser.parse(sender, body, account)?.let { parsed ->
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
                                rawMessage = rawSmsCrypto.encrypt(body)
                            )
                        }
                    }

                    if (transaction != null) {
                        database.transactionDao().insert(transaction)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
