package com.transactiontracker.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun money(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}

fun shortDate(millis: Long): String {
    return SimpleDateFormat("dd MMM, h:mm a", Locale("en", "IN")).format(Date(millis))
}
