package com.transactiontracker.sms

data class SmsSyncResult(
    val scanned: Int,
    val matchedCandidates: Int,
    val saved: Int,
    val duplicates: Int,
    val discarded: Int
)
