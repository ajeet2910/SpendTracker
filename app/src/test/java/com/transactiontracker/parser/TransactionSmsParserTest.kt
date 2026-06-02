package com.transactiontracker.parser

import com.transactiontracker.data.TrackedAccountEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransactionSmsParserTest {
    private val parser = TransactionSmsParser()

    @Test
    fun parsesHdfcAccountSentUpi() {
        val parsed = parser.parse(
            sender = "HDFCBK",
            body = """
                Sent Rs.20.00
                From HDFC Bank A/C *9305
                To Gajulapalli Jagan Mohan R
                On 29/05/26
                Ref 123xxxxxx138
                Not You?
                Call 18002586161/SMS BLOCK UPI to 7308080808
            """.trimIndent(),
            account = account("HDFC", "Bank Account", "9305", "HDFCBK")
        )

        assertNotNull(parsed)
        assertEquals(20.00, parsed!!.amount, 0.0)
        assertEquals("debit", parsed.direction)
        assertEquals("UPI", parsed.paymentCategory)
        assertEquals("GAJULAPALLI JAGAN MOHAN R", parsed.merchant)
    }

    @Test
    fun parsesHdfcCardUpiTxn() {
        val parsed = parser.parse(
            sender = "HDFCBK",
            body = """
                Txn Rs.219.45
                On HDFC Bank Card 4098
                At ctrlxtechnologiesp.cf@axi
                by UPI 1XXXXXX15461
                On 30-05
                Not You?
                Call 18002586161/SMS BLOCK CC 4098 to 7308080808
            """.trimIndent(),
            account = account("HDFC", "Credit Card", "4098", "HDFCBK")
        )

        assertNotNull(parsed)
        assertEquals(219.45, parsed!!.amount, 0.0)
        assertEquals("debit", parsed.direction)
        assertEquals("UPI", parsed.paymentCategory)
        assertEquals("CTRLXTECHNOLOGIESP.CF@AXI", parsed.merchant)
    }

    @Test
    fun parsesAxisCardSpend() {
        val parsed = parser.parse(
            sender = "AXISBK",
            body = """
                Spent INR 241
                Axis Bank Card no. XX8237
                31-05-26 18:50:52 IST
                AMAZONIN
                Avl Limit: INR 49180.96
                Not you? SMS BLOCK 8237 to 919951860002
            """.trimIndent(),
            account = account("Axis", "Credit Card", "8237", "AXIS")
        )

        assertNotNull(parsed)
        assertEquals(241.00, parsed!!.amount, 0.0)
        assertEquals("debit", parsed.direction)
        assertEquals("Credit Card", parsed.paymentCategory)
        assertEquals("AMAZONIN", parsed.merchant)
    }

    @Test
    fun parsesIdfcHappyShoppingSpend() {
        val parsed = parser.parse(
            sender = "IDFCFB",
            body = "Happy Shopping! INR 381.00 spent on your IDFC FIRST Bank Credit Card ending XX3722 at ZEPTO MARKETPLACE PRIV on 02 JAN 2026 at 01:43 PM Avbl Limit: INR 298751 If not done by you, call 180010888 for dispute or to block your card SMS CCBLOCK 3722 to 5676732",
            account = account("IDFC", "Credit Card", "3722", "IDFC")
        )

        assertNotNull(parsed)
        assertEquals(381.00, parsed!!.amount, 0.0)
        assertEquals("debit", parsed.direction)
        assertEquals("Credit Card", parsed.paymentCategory)
        assertEquals("ZEPTO MARKETPLACE PRIV", parsed.merchant)
    }

    @Test
    fun parsesIdfcTransactionSuccessfulSpend() {
        val parsed = parser.parse(
            sender = "IDFCFB",
            body = "Transaction Successful! INR 1293.70 spent on your IDFC FIRST Bank Credit Card ending XX3722 at Apollo Pharmacies Limi on 02 APR 2026 at 04:31 PM Avbl Limit: INR 294735.25 If not done by you, call 180010888 for dispute or to block your card SMS CCBLOCK 3722 to 5676732",
            account = account("IDFC", "Credit Card", "3722", "IDFC")
        )

        assertNotNull(parsed)
        assertEquals(1293.70, parsed!!.amount, 0.0)
        assertEquals("debit", parsed.direction)
        assertEquals("Credit Card", parsed.paymentCategory)
        assertEquals("APOLLO PHARMACIES LIMI", parsed.merchant)
    }

    private fun account(
        bankName: String,
        type: String,
        lastDigits: String,
        senderHints: String
    ): TrackedAccountEntity {
        return TrackedAccountEntity(
            bankName = bankName,
            type = type,
            lastDigits = lastDigits,
            nickname = bankName,
            senderHints = senderHints
        )
    }
}
