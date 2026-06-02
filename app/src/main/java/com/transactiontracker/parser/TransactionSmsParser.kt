package com.transactiontracker.parser

import com.transactiontracker.data.TrackedAccountEntity
import java.util.Locale

class TransactionSmsParser {
    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|\u20B9)\s*([0-9,]+(?:\.\d{1,2})?)""")
    private val debitRegex = Regex("""(?i)\b(debited|debit|spent|sent|paid|withdrawn|purchase|used|transaction successful|transaction of|txn|txn of)\b""")
    private val creditRegex = Regex("""(?i)\b(credited|received|refund|reversed|cashback)\b""")
    private val noiseRegex = Regex("""(?i)\b(otp|one time password|offer|pre-approved|apply now|loan|due date|statement generated|minimum amount due)\b""")
    private val merchantRegexes = listOf(
        Regex("""(?i)\bat\s+(.+?)\s+on\s+\d{1,2}\s+[a-z]{3}\s+\d{4}\b"""),
        Regex("""(?i)\bat\s+(.+?)\s+by\s+upi\b"""),
        Regex("""(?i)\bto\s+(.+?)\s+on\s+\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b"""),
        Regex("""(?i)\b\d{2}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+ist\s+(.+?)\s+avl\s+limit\b"""),
        Regex("""(?i)\bat\s+(.+?)\s+(?:avl\s+limit|avbl\s+limit|not\s+you\?)\b"""),
        Regex("""(?i)\bfor\s+(.+?)\s+(?:on|avl\s+limit|avbl\s+limit|not\s+you\?)\b""")
    )

    fun parse(sender: String, body: String, account: TrackedAccountEntity): ParsedTransaction? {
        val normalizedBody = body.replace('\n', ' ').trim()
        if (noiseRegex.containsMatchIn(normalizedBody)) return null
        if (!matchesAccount(sender, normalizedBody, account)) return null

        val amount = amountRegex.find(normalizedBody)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null

        val isDebit = debitRegex.containsMatchIn(normalizedBody)
        val isCredit = creditRegex.containsMatchIn(normalizedBody)
        if (!isDebit && !isCredit) return null

        val confidence = buildConfidence(sender, normalizedBody, account, isDebit, isCredit)
        if (confidence < 70) return null

        return ParsedTransaction(
            amount = amount,
            direction = if (isCredit && !isDebit) "credit" else "debit",
            paymentCategory = paymentCategory(normalizedBody, account),
            merchant = extractMerchant(normalizedBody),
            confidence = confidence
        )
    }

    private fun matchesAccount(sender: String, body: String, account: TrackedAccountEntity): Boolean {
        val senderHints = account.senderHints
            .split(',', ';', '|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val senderMatches = senderHints.any { sender.contains(it, ignoreCase = true) }
        val bankMatches = sender.contains(account.bankName, ignoreCase = true) ||
            body.contains(account.bankName, ignoreCase = true)
        val digitsMatch = body.contains(account.lastDigits)

        return digitsMatch && (senderMatches || bankMatches || senderHints.isEmpty())
    }

    private fun buildConfidence(
        sender: String,
        body: String,
        account: TrackedAccountEntity,
        isDebit: Boolean,
        isCredit: Boolean
    ): Int {
        var score = 0
        if (body.contains(account.lastDigits)) score += 35
        if (amountRegex.containsMatchIn(body)) score += 25
        if (isDebit || isCredit) score += 25
        if (account.senderHints.split(',', ';', '|').any { it.isNotBlank() && sender.contains(it.trim(), true) }) score += 15
        if (body.contains(account.bankName, true) || sender.contains(account.bankName, true)) score += 10
        return score.coerceAtMost(100)
    }

    private fun extractMerchant(body: String): String? {
        return merchantRegexes.firstNotNullOfOrNull { regex ->
            regex.find(body)?.groupValues?.getOrNull(1)
        }?.cleanMerchant()
            ?.take(40)
            ?.uppercase(Locale.US)
    }

    private fun paymentCategory(body: String, account: TrackedAccountEntity): String {
        return when {
            body.contains("upi", ignoreCase = true) -> "UPI"
            account.type.equals("Credit Card", ignoreCase = true) -> "Credit Card"
            account.type.equals("Debit Card", ignoreCase = true) -> "Debit Card"
            else -> account.type
        }
    }

    private fun String.cleanMerchant(): String {
        return replace(Regex("""(?i)\b(not\s+you|call|sms\s+block|avl\s+limit|avbl\s+limit).*$"""), "")
            .trim(' ', '.', ',', '-', ':')
            .replace(Regex("""\s+"""), " ")
    }
}
