package com.transactiontracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.transactiontracker.ui.TrackerApp
import com.transactiontracker.ui.theme.TransactionTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransactionTrackerTheme {
                TrackerApp()
            }
        }
    }
}
