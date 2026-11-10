package com.example.smsexpense.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsexpense.data.CategoryEntity
import com.example.smsexpense.data.TransactionEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class Period(val label: String) {
    DAY("DAY"),
    WEEK("WEEK"),
    MONTH("MONTH"),
    YEAR("YEAR"),
    CUSTOM("CUSTOM")
}

internal enum class HomePage {
    HOME,
    SETTINGS,
    CATEGORIES
}

internal data class DateRange(val start: LocalDate, val end: LocalDate)

internal data class TransactionFilters(
    val period: Period = Period.DAY,
    val date: LocalDate,
    val weekDay: LocalDate? = null,
    val customRange: DateRange? = null,
    val categories: Set<Long> = emptySet(),
    val minimumPaise: Long? = null,
    val maximumPaise: Long? = null,
    val direction: String = "all"
)

internal val Screen = Color(0xFFF8F9FB)
internal val Ink = Color(0xFF191C1E)
internal val Muted = Color(0xFF75777D)
internal val Primary = Color(0xFF182336)
internal val PrimaryContainer = Color(0xFF2E384D)
internal val Green = Color(0xFF006C4D)
internal val GreenContainer = Color(0xFFDAF5E9)
internal val Red = Color(0xFF93000A)
internal val RedContainer = Color(0xFFFFDAD6)
internal val Soft = Color(0xFFF3F4F6)
internal val Border = Color(0xFFE7E8EA)
internal val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault())
internal val shortDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    darkTheme: Boolean,
    overlayAccessGranted: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onRequestOverlayAccess: () -> Unit,
    onAssignCategory: (Long, Long?) -> Unit,
    onUpdateNote: (Long, String?) -> Unit,
    onAddCategory: (String, String) -> Unit,
    onRenameCategory: (Long, String) -> Unit,
    onDeleteCategory: (Long) -> Boolean,
    onExportCsv: (List<TransactionEntity>) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val preferences = remember(context) {
        context.getSharedPreferences("home_screen_preferences", android.content.Context.MODE_PRIVATE)
    }
    var query by rememberSaveable { mutableStateOf("") }
    var filters by remember { mutableStateOf(TransactionFilters(date = today)) }
    var filterSheetOpen by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(HomePage.HOME) }

    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val visibleTransactions = remember(transactions, categories, filters, query, today) {
        val tokens = query.trim().lowercase(Locale.getDefault())
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        transactions.filter { transaction ->
            val date = transaction.timestamp.toLocalDate()
            val category = transaction.categoryId?.let(categoriesById::get)?.name.orEmpty()
            val searchable = listOf(
                transaction.merchant.orEmpty(),
                transaction.sender,
                transaction.counterparty.orEmpty(),
                transaction.type,
                transaction.note.orEmpty(),
                transaction.accountLast4.orEmpty(),
                transaction.upiReference.orEmpty(),
                transaction.rawBody,
                category,
                formatMoney(transaction.amount),
                (transaction.amount / 100.0).toString()
            ).joinToString(" ").lowercase(Locale.getDefault())
            tokens.all(searchable::contains) &&
                filters.matchesDate(date, today) &&
                (filters.categories.isEmpty() || transaction.categoryId in filters.categories) &&
                (filters.direction == "all" ||
                    (filters.direction == "debit" && transaction.type.equals("DEBIT", true)) ||
                    (filters.direction == "credit" && transaction.type.equals("CREDIT", true))) &&
                (filters.minimumPaise?.let { transaction.amount >= it } ?: true) &&
                (filters.maximumPaise?.let { transaction.amount <= it } ?: true)
        }
    }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        BackHandler(enabled = page != HomePage.HOME) {
            page = if (page == HomePage.CATEGORIES) HomePage.SETTINGS else HomePage.HOME
        }
        val background = if (darkTheme) Color(0xFF111827) else Screen
        val text = if (darkTheme) Color(0xFFF3F4F6) else Ink
        val muted = if (darkTheme) Color(0xFFADB5C2) else Muted
        val card = if (darkTheme) Color(0xFF1F2937) else Color.White
        val soft = if (darkTheme) Color(0xFF273449) else Soft

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page == HomePage.HOME) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(soft, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⌂", color = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "● SMS Expense Tracker",
                                color = if (darkTheme) Color(0xFF93F6CA) else Green,
                                fontSize = 10.sp,
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Transactions", color = text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { page = HomePage.SETTINGS }
                                .semantics { contentDescription = "Open settings" },
                            shape = CircleShape,
                            color = Primary
                        ) {
                            UserIcon()
                        }
                    } else {
                        TextButton(onClick = {
                            page = if (page == HomePage.CATEGORIES) HomePage.SETTINGS else HomePage.HOME
                        }) {
                            Text("‹  Back", color = text)
                        }
                        Text(
                            if (page == HomePage.SETTINGS) "Settings" else "Categories",
                            color = text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (page == HomePage.HOME) LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SearchBar(
                            query = query,
                            card = card,
                            muted = muted,
                            onQueryChange = { query = it },
                            onFiltersClick = { filterSheetOpen = true }
                        )
                    }
                    item {
                        SummarySection(
                            transactions = visibleTransactions,
                            categoriesById = categoriesById,
                            filters = filters,
                            today = today,
                            text = text,
                            muted = muted,
                            query = query,
                            darkTheme = darkTheme
                        )
                    }
                    if (visibleTransactions.isEmpty()) {
                        item { EmptyTransactions(query.isNotBlank(), card, text, muted) }
                    } else {
                        items(visibleTransactions, key = { it.id }) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                categoryName = transaction.categoryId?.let(categoriesById::get)?.name,
                                categories = categories,
                                card = card,
                                text = text,
                                muted = muted,
                                soft = soft,
                                onAssignCategory = { categoryId ->
                                    onAssignCategory(transaction.id, categoryId)
                                },
                                onUpdateNote = { note -> onUpdateNote(transaction.id, note) }
                            )
                        }
                    }
                    item {
                        OfflineFooter(
                            soft = soft,
                            text = text,
                            muted = muted,
                            onExportCsv = { onExportCsv(visibleTransactions) }
                        )
                    }
                } else if (page == HomePage.SETTINGS) {
                        SettingsPage(
                            darkTheme = darkTheme,
                            overlayAccessGranted = overlayAccessGranted,
                            card = card,
                            text = text,
                            muted = muted,
                            onDarkThemeChange = {
                                onDarkThemeChange(it)
                                preferences.edit().putBoolean("dark_theme", it).apply()
                            },
                            onRequestOverlayAccess = onRequestOverlayAccess,
                            onManageCategories = { page = HomePage.CATEGORIES }
                        )
                    } else {
                        CategoryManagementPage(
                            categories = categories,
                            transactions = transactions,
                            card = card,
                            text = text,
                            muted = muted,
                            onAdd = onAddCategory,
                            onRename = onRenameCategory,
                            onDelete = onDeleteCategory
                        )
                    }
            }
        }

        if (filterSheetOpen) {
            FilterSheet(
                filters = filters,
                categories = categories,
                today = today,
                onDismiss = { filterSheetOpen = false },
                onApply = {
                    filters = it
                    filterSheetOpen = false
                }
            )
        }
    }
}
