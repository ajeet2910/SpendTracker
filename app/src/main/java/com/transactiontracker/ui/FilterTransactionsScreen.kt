package com.transactiontracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transactiontracker.data.TransactionEntity
import java.time.Instant
import java.time.Month
import java.time.ZoneId

private enum class TransactionSort(val label: String) {
    TimestampNewest("Timestamp"),
    AmountHigh("Amount High"),
    AmountLow("Amount Low"),
    CreditFirst("Credit First"),
    DebitFirst("Debit First")
}

private data class MerchantOption(
    val label: String,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTransactionsScreen(viewModel: FilterTransactionsViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedMonth by remember { mutableStateOf<Int?>(null) }
    var selectedCard by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedMerchant by remember { mutableStateOf<String?>(null) }
    var visibleMerchantCount by remember { mutableStateOf(10) }
    var selectedDirection by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(TransactionSort.TimestampNewest) }

    val years = transactions
        .map { it.occurredAt.yearPart() }
        .distinct()
        .sortedDescending()

    val cards = transactions
        .map { it.cardFilterLabel() to it.lastDigits }
        .distinct()
        .sortedBy { it.first }

    val selectedCardLabel = cards.firstOrNull { it.second == selectedCard }?.first ?: "All"
    val categories = transactions
        .map { it.paymentCategory }
        .distinct()
        .sorted()
    val merchantSourceTransactions = transactions
        .asSequence()
        .filter { selectedYear == null || it.occurredAt.yearPart() == selectedYear }
        .filter { selectedMonth == null || it.occurredAt.monthPart() == selectedMonth }
        .toList()

    val merchants = merchantSourceTransactions
        .mapNotNull { it.merchantFilterLabel() }
        .groupingBy { it }
        .eachCount()
        .map { MerchantOption(label = it.key, count = it.value) }
        .sortedWith(compareByDescending<MerchantOption> { it.count }.thenBy { it.label })

    val filtered = transactions
        .asSequence()
        .filter { selectedYear == null || it.occurredAt.yearPart() == selectedYear }
        .filter { selectedMonth == null || it.occurredAt.monthPart() == selectedMonth }
        .filter { selectedCard == null || it.lastDigits == selectedCard }
        .filter { selectedCategory == null || it.paymentCategory == selectedCategory }
        .filter { selectedMerchant == null || it.merchantFilterLabel() == selectedMerchant }
        .filter { selectedDirection == null || it.direction == selectedDirection }
        .let { sequence ->
            when (sort) {
                TransactionSort.TimestampNewest -> sequence.sortedByDescending { it.occurredAt }
                TransactionSort.AmountHigh -> sequence.sortedByDescending { it.amount }
                TransactionSort.AmountLow -> sequence.sortedBy { it.amount }
                TransactionSort.CreditFirst -> sequence.sortedWith(
                    compareByDescending<TransactionEntity> { it.direction == "credit" }
                        .thenByDescending { it.occurredAt }
                )
                TransactionSort.DebitFirst -> sequence.sortedWith(
                    compareByDescending<TransactionEntity> { it.direction == "debit" }
                        .thenByDescending { it.occurredAt }
                )
            }
        }
        .toList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Txn History Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        DropdownField(
                            label = "Year",
                            value = selectedYear?.toString() ?: "All",
                            options = listOf("All") + years.map { it.toString() },
                            onSelect = {
                                selectedYear = it.takeUnless { value -> value == "All" }?.toIntOrNull()
                                selectedMerchant = null
                                visibleMerchantCount = 10
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DropdownField(
                            label = "Month",
                            value = selectedMonth?.let { Month.of(it).name.take(3) } ?: "All",
                            options = listOf("All") + (1..12).map { Month.of(it).name.take(3) },
                            onSelect = { value ->
                                selectedMonth = if (value == "All") {
                                    null
                                } else {
                                    (1..12).first { Month.of(it).name.take(3) == value }
                                }
                                selectedMerchant = null
                                visibleMerchantCount = 10
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        DropdownField(
                            label = "Card Number",
                            value = selectedCardLabel,
                            options = listOf("All") + cards.map { it.first },
                            onSelect = { value -> selectedCard = cards.firstOrNull { it.first == value }?.second },
                            modifier = Modifier.weight(1f)
                        )

                        DropdownField(
                            label = "Category",
                            value = selectedCategory ?: "All",
                            options = listOf("All") + categories,
                            onSelect = { value -> selectedCategory = value.takeUnless { it == "All" } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MerchantDropdownField(
                        value = selectedMerchant ?: "All",
                        merchants = merchants,
                        visibleCount = visibleMerchantCount,
                        onSelect = { selectedMerchant = it },
                        onLoadMore = { visibleMerchantCount += 10 }
                    )

                    DropdownField(
                        label = "Type",
                        value = selectedDirection?.replaceFirstChar { it.uppercase() } ?: "All",
                        options = listOf("All", "Credit", "Debit"),
                        onSelect = { value -> selectedDirection = value.takeUnless { it == "All" }?.lowercase() }
                    )

                    DropdownField(
                        label = "Sort",
                        value = sort.label,
                        options = TransactionSort.entries.map { it.label },
                        onSelect = { value -> sort = TransactionSort.entries.first { it.label == value } }
                    )
                }
            }
        }

        item {
            Text(
                "${filtered.size} transaction${if (filtered.size == 1) "" else "s"} found",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text("No transactions match these filters.")
            }
        }

        items(filtered, key = { it.id }) { transaction ->
            FilteredTransactionCard(
                transaction = transaction,
                rawMessage = viewModel.rawMessageFor(transaction),
                onIgnoredChange = { ignored ->
                    viewModel.setIgnoredInTotals(transaction.id, ignored)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantDropdownField(
    value: String,
    merchants: List<MerchantOption>,
    visibleCount: Int,
    onSelect: (String?) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleMerchants = merchants.take(visibleCount)
    val hasMore = merchants.size > visibleCount

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Merchant") },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            visibleMerchants.forEach { merchant ->
                DropdownMenuItem(
                    text = { Text("${merchant.label} (${merchant.count})") },
                    onClick = {
                        onSelect(merchant.label)
                        expanded = false
                    }
                )
            }
            if (hasMore) {
                DropdownMenuItem(
                    text = { Text("More") },
                    onClick = { onLoadMore() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilteredTransactionCard(
    transaction: TransactionEntity,
    rawMessage: String?,
    onIgnoredChange: (Boolean) -> Unit
) {
    var expanded by remember(transaction.id) { mutableStateOf(false) }

    Card(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    transaction.merchant ?: transaction.bankName,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    money(transaction.amount),
                    color = if (transaction.ignoredInTotals) {
                        MaterialTheme.colorScheme.outline
                    } else if (transaction.direction == "credit") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (transaction.ignoredInTotals) TextDecoration.LineThrough else null
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Hide raw message" else "Show raw message"
                )
            }
            Text("${transaction.direction.replaceFirstChar { it.uppercase() }} - ${transaction.paymentCategory} - ${transaction.accountType} ending ${transaction.lastDigits}")
            Text(shortDate(transaction.occurredAt), style = MaterialTheme.typography.bodySmall)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    Text(
                        rawMessage ?: "Raw SMS was not saved for this transaction.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FilterChip(
                        selected = transaction.ignoredInTotals,
                        onClick = { onIgnoredChange(!transaction.ignoredInTotals) },
                        label = {
                            Text(if (transaction.ignoredInTotals) "Ignored" else "Ignore Txn")
                        }
                    )
                }
            }
        }
    }
}

private fun Long.yearPart(): Int {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).year
}

private fun Long.monthPart(): Int {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).monthValue
}

private fun TransactionEntity.cardFilterLabel(): String {
    val compactBankName = bankName
        .replace("Bank", "", ignoreCase = true)
        .replace(Regex("""\s+"""), " ")
        .trim()
        .ifBlank { bankName }

    return "$compactBankName $lastDigits"
}

private fun TransactionEntity.merchantFilterLabel(): String? {
    return merchant
        ?.trim()
        ?.replace(Regex("""\s+"""), " ")
        ?.takeIf { it.isNotBlank() }
}
