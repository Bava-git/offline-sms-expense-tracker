package com.example.smsexpense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsexpense.data.CategoryEntity
import com.example.smsexpense.data.TransactionEntity
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
internal fun SearchBar(
    query: String,
    card: Color,
    muted: Color,
    onQueryChange: (String) -> Unit,
    onFiltersClick: () -> Unit
) {
    Surface(color = card, shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌕", color = muted, fontSize = 23.sp, lineHeight = 24.sp)
            Spacer(Modifier.width(9.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search merchant, category, or amount...",
                        color = muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) { Text("CLEAR") }
            }
            TextButton(
                onClick = onFiltersClick,
                modifier = Modifier.semantics { contentDescription = "Open transaction filters" }
            ) {
                Text("☷")
            }
        }
    }
}

@Composable
internal fun SummarySection(
    transactions: List<TransactionEntity>,
    categoriesById: Map<Long, CategoryEntity>,
    filters: TransactionFilters,
    today: LocalDate,
    text: Color,
    muted: Color,
    query: String,
    darkTheme: Boolean
) {
    val debits = transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
    val credits = transactions.filter { it.type.equals("CREDIT", ignoreCase = true) }
    val debited = debits.sumOf { it.amount }
    val credited = credits.sumOf { it.amount }
    val net = credited - debited
    val activeCount = filters.activeCount(today)

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(filters.heading(today), color = text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${filters.dateLabel(today)} · ${transactions.size} ${if (transactions.size == 1) "transaction" else "transactions"}${if (query.isNotBlank()) " · search applied" else ""}",
                    color = muted,
                    fontSize = 10.sp
                )
            }
            Surface(color = if (darkTheme) Color(0xFF1F2937) else GreenContainer, shape = CircleShape) {
                Text(
                    if (activeCount == 0) "SYNCED" else "FILTERS $activeCount",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = if (activeCount == 0) Green else text,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = if (darkTheme) Color(0xFF111827) else Primary),
            shape = RoundedCornerShape(15.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(filters.aggregateTitle(today), color = Color(0xFFBCC6E1), fontSize = 9.sp, letterSpacing = 0.8.sp)
                    Text("LOCAL", color = Color(0xFFBCC6E1), fontSize = 9.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SummaryMetric(
                        "DEBITED", formatMoney(debited), "${debits.size} debits",
                        Color(0xFFFFDAD6), Modifier.weight(1f)
                    )
                    SummaryMetric(
                        "CREDITED", "+${formatMoney(credited)}", "${credits.size} credits",
                        Color(0xFF93F6CA), Modifier.weight(1f)
                    )
                    SummaryMetric(
                        "NET DELTA",
                        signedMoney(net),
                        if (net >= 0) "Surplus" else "Net outflow",
                        Color.White,
                        Modifier.weight(1f)
                    )
                }
            }
        }
        if (filters.categories.isNotEmpty()) {
            Text(
                "Categories: " + filters.categories.mapNotNull { categoriesById[it]?.name }.joinToString(),
                color = muted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
internal fun SummaryMetric(
    label: String,
    amount: String,
    detail: String,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(PrimaryContainer)
            .padding(horizontal = 7.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color(0xFFBCC6E1), fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            amount,
            color = amountColor,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )
        Text(detail, color = Color(0xFFC5C6CD), fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
internal fun TransactionCard(
    transaction: TransactionEntity,
    categoryName: String?,
    categories: List<CategoryEntity>,
    card: Color,
    text: Color,
    muted: Color,
    soft: Color,
    onAssignCategory: (Long?) -> Unit,
    onUpdateNote: (String?) -> Unit
) {
    var expanded by rememberSaveable(transaction.id) { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var transactionMenuOpen by remember { mutableStateOf(false) }
    var noteDialogOpen by rememberSaveable(transaction.id) { mutableStateOf(false) }
    var noteText by remember(transaction.id, transaction.note) { mutableStateOf(transaction.note.orEmpty()) }
    val debit = transaction.type.equals("DEBIT", true)
    val transactionAccent = if (debit) Red else Green
    val transactionTint = if (debit) {
        if (soft == Color(0xFF273449)) Color(0xFF3B2630) else Color(0xFFFFF1F0)
    } else {
        if (soft == Color(0xFF273449)) Color(0xFF1F3A32) else Color(0xFFECF8F2)
    }
    val merchant = transaction.merchant?.takeIf(String::isNotBlank) ?: transaction.sender.ifBlank { "BANK SMS" }
    val time = remember(transaction.timestamp) {
        java.text.SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    }
    val channel = transaction.rawBody.detectChannel()
    val reference = transaction.upiReference ?: transaction.rawBody.detectReference()

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${merchant.uppercase(Locale.getDefault())} · $time",
                    color = muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    TextButton(
                        onClick = { transactionMenuOpen = true },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = "Transaction options" },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("⋮", color = muted, fontSize = 24.sp, lineHeight = 24.sp)
                    }
                    DropdownMenu(
                        expanded = transactionMenuOpen,
                        onDismissRequest = { transactionMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (transaction.note.isNullOrBlank()) "Add note" else "Edit note") },
                            onClick = {
                                noteText = transaction.note.orEmpty()
                                noteDialogOpen = true
                                transactionMenuOpen = false
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.counterparty?.takeIf(String::isNotBlank)
                            ?: transaction.sender.ifBlank { "Unknown sender" },
                        color = text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        transaction.accountLast4
                            ?.takeIf(String::isNotBlank)
                            ?.let { "Acct  XXX$it" }
                            ?: "Acct number not detected",
                        color = muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = transactionTint,
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                transaction.type.ifBlank { "TRANSACTION" },
                                color = transactionAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Box {
                            Text(
                                categoryName ?: "Add category",
                                color = if (categoryName == null) muted else muted,
                                fontSize = 9.sp,
                                modifier = Modifier.clickable { categoryMenuOpen = true }.padding(3.dp)
                            )
                            DropdownMenu(
                                expanded = categoryMenuOpen,
                                onDismissRequest = { categoryMenuOpen = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text("${category.icon}  ${category.name}") },
                                        onClick = {
                                            onAssignCategory(category.id)
                                            categoryMenuOpen = false
                                        }
                                    )
                                }
                                if (transaction.categoryId != null) {
                                    DropdownMenuItem(
                                        text = { Text("Remove category") },
                                        onClick = {
                                            onAssignCategory(null)
                                            categoryMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = transactionTint
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AMOUNT", color = muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${if (debit) "−" else "+"}${formatMoney(transaction.amount)}",
                        color = transactionAccent,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            reference?.let {
                Text("UPI Ref No.  $it", color = muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            transaction.note?.takeIf(String::isNotBlank)?.let {
                Text("Note: $it", color = muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(soft, RoundedCornerShape(9.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    transaction.balanceAfter?.let { Text("Balance after  ${formatMoney(it)}", color = muted, fontSize = 10.sp) }
                    channel?.let { Text("Channel  $it", color = muted, fontSize = 10.sp) }
                    Text("SMS details", color = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text(transaction.rawBody, color = muted, fontSize = 10.sp)
                }
            }
        }
    }
    if (noteDialogOpen) {
        AlertDialog(
            onDismissRequest = { noteDialogOpen = false },
            title = { Text(if (transaction.note.isNullOrBlank()) "Add note" else "Edit note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
            },
            dismissButton = {
                TextButton(onClick = { noteDialogOpen = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateNote(noteText)
                    noteDialogOpen = false
                }) { Text("Save") }
            }
        )
    }
}

@Composable
internal fun EmptyTransactions(hasQuery: Boolean, card: Color, text: Color, muted: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().background(card, RoundedCornerShape(14.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (hasQuery) "No matching transactions" else "No transactions for this period", color = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(
            if (hasQuery) "Try a different merchant, category, or amount." else "Adjust the filters or check another date range.",
            color = muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun OfflineFooter(soft: Color, text: Color, muted: Color, onExportCsv: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(soft, RoundedCornerShape(13.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(GreenContainer, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) { Text("✓", color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Checksum validated", color = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Local transaction data", color = muted, fontSize = 9.sp)
        }
        TextButton(onClick = onExportCsv) { Text("Export CSV") }
    }
}
