package com.transactiontracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7A5F),
    onPrimary = Color.White,
    secondary = Color(0xFF455A64),
    surface = Color(0xFFFFFBF6),
    background = Color(0xFFFFFBF6),
    surfaceVariant = Color(0xFFE3E6DE),
    primaryContainer = Color(0xFFCDEFE3),
    onPrimaryContainer = Color(0xFF06251D)
)

@Composable
fun TransactionTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
