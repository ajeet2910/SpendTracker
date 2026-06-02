package com.transactiontracker.parser

import java.security.MessageDigest

fun messageHash(sender: String, body: String, receivedAt: Long): String {
    val input = "$sender|$receivedAt|${body.trim()}"
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
