package com.spendsense.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendsense.app.SpendViewModel
import com.spendsense.app.data.Category
import com.spendsense.app.data.Transaction
import com.spendsense.app.data.TxnType
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val inr: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dateFmt = DateTimeFormatter.ofPattern("d MMM, h:mm a")
private val zone: ZoneId = ZoneId.systemDefault()

private fun money(v: Double) = inr.format(v)

@Composable
fun SpendScreen(vm: SpendViewModel) {
    val state by vm.state.collectAsState()

    val monthTxns = remember(state.transactions, state.month) {
        state.transactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == state.month
        }
    }
    val debits = monthTxns.filter { it.type == TxnType.DEBIT }
    val spent = debits.sumOf { it.amount }
    val received = monthTxns.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
    val byCategory = debits.groupBy { it.category }
        .map { (cat, list) -> cat to list.sumOf { it.amount } }
        .sortedByDescending { it.second }
    val shown = state.categoryFilter?.let { f -> monthTxns.filter { it.category == f } } ?: monthTxns

    var editing by remember { mutableStateOf<Transaction?>(null) }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = vm::previousMonth) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                    Text(
                        state.month.format(monthFmt),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = vm::nextMonth, enabled = state.month < YearMonth.now()) {
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                    TextButton(onClick = vm::load) { Text("Refresh") }
                }
            }

            item { SummaryCard(spent, received, monthTxns.size) }

            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { err -> item { Text(err, color = MaterialTheme.colorScheme.error) } }

            if (byCategory.isNotEmpty()) {
                item { SectionTitle("Spending by category") }
                items(byCategory, key = { "cat-" + it.first.name }) { (cat, amount) ->
                    CategoryRow(cat, amount, spent, selected = state.categoryFilter == cat) {
                        vm.toggleFilter(cat)
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(
                        state.categoryFilter?.let { "${it.emoji} ${it.label}" } ?: "Transactions",
                        Modifier.weight(1f),
                    )
                    if (state.categoryFilter != null) {
                        TextButton(onClick = { vm.toggleFilter(state.categoryFilter!!) }) { Text("Show all") }
                    }
                }
            }

            if (shown.isEmpty() && !state.loading) {
                item { Text("No bank transactions found for this month.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            items(shown, key = { it.smsId }) { t ->
                TransactionRow(t) { editing = t }
                HorizontalDivider()
            }
        }
    }

    editing?.let { t ->
        CategoryDialog(
            txn = t,
            onDismiss = { editing = null },
            onPick = { cat -> vm.recategorize(t.merchant, cat); editing = null },
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SummaryCard(spent: Double, received: Double, count: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Spent", style = MaterialTheme.typography.labelLarge)
            Text(money(spent), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row {
                Text("Received ${money(received)}", color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                Text("$count transactions", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CategoryRow(cat: Category, amount: Double, total: Double, selected: Boolean, onClick: () -> Unit) {
    val fraction = if (total > 0) (amount / total).toFloat() else 0f
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row {
                Text("${cat.emoji}  ${cat.label}", modifier = Modifier.weight(1f))
                Text(money(amount), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TransactionRow(t: Transaction, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t.category.emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.merchant, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                Instant.ofEpochMilli(t.timestamp).atZone(zone).format(dateFmt) +
                    " · ${t.bank}" + (t.account?.let { " ••$it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val isDebit = t.type == TxnType.DEBIT
        Text(
            (if (isDebit) "−" else "+") + money(t.amount),
            color = if (isDebit) MaterialTheme.colorScheme.onSurface else Color(0xFF2E7D32),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CategoryDialog(txn: Transaction, onDismiss: () -> Unit, onPick: (Category) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(txn.merchant) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(txn.rawBody, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text("Move all \"${txn.merchant}\" transactions to:", style = MaterialTheme.typography.labelLarge)
                Category.entries.forEach { cat ->
                    Text(
                        "${cat.emoji}  ${cat.label}" + if (cat == txn.category) "  ✓" else "",
                        modifier = Modifier.fillMaxWidth().clickable { onPick(cat) }.padding(vertical = 10.dp),
                        fontWeight = if (cat == txn.category) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        },
    )
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SpendSense", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "To track spending, the app reads bank SMS on your phone. Messages are processed " +
                "on-device only and never leave your phone.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Allow SMS access") }
    }
}
