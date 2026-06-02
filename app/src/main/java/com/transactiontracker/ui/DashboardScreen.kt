package com.transactiontracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transactiontracker.data.TransactionEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val lastThreeMonthsTransactions by viewModel.lastThreeMonthsTransactions.collectAsState()
    val month by viewModel.month.collectAsState()
    val debits = transactions.debitTotal()
    val credits = transactions.creditTotal()
    val byCategory = transactions
        .filter { it.direction == "debit" && !it.ignoredInTotals }
        .groupBy { it.paymentCategory }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Monthly Spend", style = MaterialTheme.typography.titleMedium)
                    Text(money(debits), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${transactions.size} saved transactions")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTile("Credits", money(credits), Modifier.weight(1f))
                SummaryTile("Net Outflow", money((debits - credits).coerceAtLeast(0.0)), Modifier.weight(1f))
            }
        }

        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Spend by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (byCategory.isEmpty()) {
                        Text("No spending found for this month.")
                    } else {
                        byCategory.forEach { (category, amount) ->
                            SpendBar(label = category, amount = amount, max = byCategory.first().second)
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Last 3 Months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    ThreeMonthSpendChart(
                        selectedMonth = month,
                        transactions = lastThreeMonthsTransactions
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SpendBar(label: String, amount: Double, max: Double) {
    val progress = if (max <= 0.0) 0f else (amount / max).toFloat().coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(money(amount))
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun ThreeMonthSpendChart(
    selectedMonth: YearMonth,
    transactions: List<TransactionEntity>
) {
    val months = listOf(
        selectedMonth.minusMonths(3),
        selectedMonth.minusMonths(2),
        selectedMonth.minusMonths(1)
    )
    val totals = months.map { chartMonth ->
        chartMonth to transactions
            .filter {
                it.direction == "debit" &&
                    !it.ignoredInTotals &&
                    YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(ZoneId.systemDefault())) == chartMonth
            }
            .sumOf { it.amount }
    }
    val max = totals.maxOfOrNull { it.second } ?: 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        totals.forEach { (chartMonth, total) ->
            val barHeight = if (max <= 0.0) {
                8.dp
            } else {
                (112 * (total / max).coerceIn(0.05, 1.0)).toFloat().dp
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(money(total), style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    chartMonth.format(DateTimeFormatter.ofPattern("MMM")),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
