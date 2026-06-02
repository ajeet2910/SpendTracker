package com.transactiontracker.parser

data class ParsedTransaction(
    val amount: Double,
    val direction: String,
    val paymentCategory: String,
    val merchant: String?,
    val confidence: Int
)
