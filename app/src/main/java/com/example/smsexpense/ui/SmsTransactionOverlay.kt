package com.example.smsexpense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import java.util.Locale

data class SmsTransactionDraft(
    val sender: String = "HDFC Bank",
    val receivedAt: String = "Just now",
    val direction: Direction = Direction.DEBIT,
    val merchant: String = "SWIGGY",
    val upiReference: String? = "432091",
    val accountLastFour: String? = "1234",
    val amount: Double = 450.0,
)

enum class Direction {
    DEBIT,
    CREDIT,
}

data class CategorizedSmsTransaction(
    val source: SmsTransactionDraft,
    val category: String,
    val note: String?,
    val shouldAutoMapMerchant: Boolean,
)

private data class CategorySuggestion(
    val name: String,
    val emoji: String,
)

private val defaultSuggestions = listOf(
    CategorySuggestion("Food & Dining", "🍔"),
    CategorySuggestion("Groceries", "🛒"),
    CategorySuggestion("Cafe", "☕"),
    CategorySuggestion("Delivery", "🛵"),
    CategorySuggestion("Transport", "🚕"),
    CategorySuggestion("Fuel", "⛽"),
    CategorySuggestion("Shopping", "🛍️"),
    CategorySuggestion("Bills & Utilities", "💡"),
)

private val rupeeFormatter = NumberFormat.getCurrencyInstance(
    Locale.forLanguageTag("en-IN"),
).apply {
    currency = java.util.Currency.getInstance("INR")
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

/**
 * An offline SMS review surface intended for an app dialog or overlay host.
 *
 * This composable provides review controls and callbacks; the host app owns
 * ledger persistence, notification/SMS permissions, and any system overlay.
 */
@Composable
fun SmsTransactionOverlay(
    transaction: SmsTransactionDraft = SmsTransactionDraft(),
    categories: List<String> = defaultSuggestions.map(CategorySuggestion::name),
    suggestedCategory: String = "",
    merchantAutoMapAvailable: Boolean = true,
    onSaveLedger: (CategorizedSmsTransaction) -> Unit,
    onRemindLater: (SmsTransactionDraft) -> Unit,
    onMinimize: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategory by remember(transaction, suggestedCategory, categories) {
        mutableStateOf(suggestedCategory.takeIf { it in categories }.orEmpty())
    }
    var quickNote by remember(transaction) { mutableStateOf("") }
    var autoMap by remember(transaction) { mutableStateOf(merchantAutoMapAvailable) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE6182336),
                            Color(0xF210141D),
                            Color(0xFA070B12),
                        ),
                    ),
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OverlayHeader(onMinimize = onMinimize, onDismiss = onDismiss)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✉", color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            transaction.sender,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            " · ${transaction.receivedAt}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (transaction.direction == Direction.DEBIT) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                        ) {
                            Text(
                                transaction.direction.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (transaction.direction == Direction.DEBIT) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("₹", fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text(
                                    transaction.merchant,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val details = listOfNotNull(
                                    transaction.upiReference?.let { "UPI/$it" },
                                    transaction.accountLastFour?.let { "a/c ••$it" },
                                ).joinToString(" · ")
                                if (details.isNotBlank()) {
                                    Text(
                                        details,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Text(
                            rupeeFormatter.format(kotlin.math.abs(transaction.amount)),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Choose a category",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (selectedCategory.isBlank()) {
                                    "Select one to save this transaction."
                                } else {
                                    "Selected: $selectedCategory"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (categories.isEmpty()) {
                            Text(
                                "No categories are available for this transaction.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { categoryMenuExpanded = true },
                                ) {
                                    Text(
                                        selectedCategory.ifBlank { "Select category" },
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    )
                                    Text("▾")
                                }
                                DropdownMenu(
                                    expanded = categoryMenuExpanded,
                                    onDismissRequest = { categoryMenuExpanded = false },
                                ) {
                                    categories.forEach { category ->
                                        val emoji = defaultSuggestions
                                            .firstOrNull { it.name == category }
                                            ?.emoji
                                            .orEmpty()
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    listOf(emoji, category)
                                                        .filter(String::isNotBlank)
                                                        .joinToString(" "),
                                                )
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                categoryMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quickNote,
                        onValueChange = { quickNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text("Add quick note (e.g. Lunch with team)...")
                        },
                        leadingIcon = { Text("✎") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    )

                    if (merchantAutoMapAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = autoMap,
                                onCheckedChange = { autoMap = it },
                            )
                            Text(
                                "Auto-map ${transaction.merchant} to $selectedCategory (Local DB)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            onClick = { onRemindLater(transaction) },
                        ) {
                            Text("◷  Remind Later")
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            enabled = selectedCategory.isNotBlank(),
                            onClick = {
                                onSaveLedger(
                                    CategorizedSmsTransaction(
                                        source = transaction,
                                        category = selectedCategory,
                                        note = quickNote.trim().takeIf(String::isNotEmpty),
                                        shouldAutoMapMerchant =
                                            merchantAutoMapAvailable && autoMap,
                                    ),
                                )
                            },
                        ) {
                            Text("✓  SAVE LEDGER")
                        }
                    }

                    Column {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "▣  Saved on this device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                "Close to return to your app",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayHeader(
    onMinimize: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Spacer(Modifier.width(6.dp))
        Text(
            "SMS Expense Tracker",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                "On device",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onMinimize,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
        ) {
            Text("Later")
        }
        TextButton(
            onClick = onDismiss,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
        ) {
            Text("×", fontSize = 20.sp)
        }
    }
}
