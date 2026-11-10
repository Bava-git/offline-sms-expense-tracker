package com.example.smsexpense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smsexpense.data.CategoryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun FilterSheet(
    filters: TransactionFilters,
    categories: List<CategoryEntity>,
    today: LocalDate,
    onDismiss: () -> Unit,
    onApply: (TransactionFilters) -> Unit
) {
    var draft by remember { mutableStateOf(filters.copy(categories = filters.categories.toSet())) }
    var minimumText by remember(draft.minimumPaise) { mutableStateOf(draft.minimumPaise?.let(::paiseToInput).orEmpty()) }
    var maximumText by remember(draft.maximumPaise) { mutableStateOf(draft.maximumPaise?.let(::paiseToInput).orEmpty()) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val minValue = parsePaise(minimumText)
    val maxValue = parsePaise(maximumText)
    val customRange = draft.customRange
    val filterValidationMessage = when {
        minimumText.isNotBlank() && minValue == null -> "Enter a valid minimum amount."
        maximumText.isNotBlank() && maxValue == null -> "Enter a valid maximum amount."
        minValue != null && maxValue != null && minValue > maxValue ->
            "Minimum amount must not exceed maximum."
        draft.period == Period.CUSTOM &&
            (customRange == null || customRange.start.isAfter(customRange.end) || customRange.end.isAfter(today)) ->
            "Select a valid date range up to today."
        else -> null
    }
    val canApply = filterValidationMessage == null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (MaterialTheme.colorScheme.background == Color(0xFF121212)) {
                Color(0xFF111827)
            } else {
                Color(0xFFF1F4F8)
            },
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Transaction filters", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Refine your local transaction list",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable(onClick = onDismiss)
                            .semantics { contentDescription = "Close filters without applying" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("×", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (MaterialTheme.colorScheme.background == Color(0xFF121212)) {
                                Color(0xFF273449)
                            } else {
                                Color(0xFFE5EAF1)
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ACTIVE FILTERS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(shape = CircleShape, color = Primary) {
                        Text(
                            draft.activeCount(today).toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
            FilterPanel(title = "DATE") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Period.values().forEach { period ->
                        FilterChip(
                            selected = draft.period == period,
                            onClick = {
                                draft = draft.copy(
                                    period = period,
                                    customRange = if (period == Period.CUSTOM) draft.customRange ?: DateRange(today, today) else draft.customRange
                                )
                            },
                            label = { Text(period.label) }
                        )
                    }
                }
                when (draft.period) {
                    Period.DAY -> {
                        PresetRow(
                            first = "Today",
                            firstSelected = draft.date == today,
                            second = "Yesterday",
                            secondSelected = draft.date == today.minusDays(1),
                            onFirst = { draft = draft.copy(date = today) },
                            onSecond = { draft = draft.copy(date = today.minusDays(1)) }
                        )
                        DateStepper(
                            label = draft.date.format(dateFormatter),
                            canNext = draft.date.isBefore(today),
                            onPrevious = { draft = draft.copy(date = draft.date.minusDays(1)) },
                            onNext = { if (draft.date.isBefore(today)) draft = draft.copy(date = draft.date.plusDays(1)) }
                        )
                    }
                    Period.WEEK -> {
                        val currentWeek = today.startOfWeek()
                        val selectedWeek = draft.date.startOfWeek()
                        PresetRow(
                            first = "This Week",
                            firstSelected = selectedWeek == currentWeek,
                            second = "Last Week",
                            secondSelected = selectedWeek == currentWeek.minusWeeks(1),
                            onFirst = { draft = draft.copy(date = today, weekDay = null) },
                            onSecond = { draft = draft.copy(date = today.minusWeeks(1), weekDay = null) }
                        )
                        DateStepper(
                            label = "${selectedWeek.format(shortDateFormatter)} – ${minOf(selectedWeek.plusDays(6), today).format(shortDateFormatter)}",
                            canNext = selectedWeek.isBefore(currentWeek),
                            onPrevious = { draft = draft.copy(date = draft.date.minusWeeks(1), weekDay = null) },
                            onNext = { if (selectedWeek.isBefore(currentWeek)) draft = draft.copy(date = draft.date.plusWeeks(1), weekDay = null) }
                        )
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (0L..6L).forEach { offset ->
                                val day = selectedWeek.plusDays(offset)
                                FilterChip(
                                    selected = draft.weekDay == day,
                                    enabled = !day.isAfter(today),
                                    onClick = { draft = draft.copy(weekDay = if (draft.weekDay == day) null else day) },
                                    label = { Text("${day.dayOfWeek.name.take(3)} ${day.dayOfMonth}") }
                                )
                            }
                        }
                    }
                    Period.MONTH -> {
                        val month = draft.date.withDayOfMonth(1)
                        val currentMonth = today.withDayOfMonth(1)
                        PresetRow(
                            first = "This Month",
                            firstSelected = month == currentMonth,
                            second = "Last Month",
                            secondSelected = month == currentMonth.minusMonths(1),
                            onFirst = { draft = draft.copy(date = today) },
                            onSecond = { draft = draft.copy(date = today.minusMonths(1)) }
                        )
                        DateStepper(
                            label = draft.date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            canNext = month.isBefore(currentMonth),
                            onPrevious = { draft = draft.copy(date = draft.date.minusMonths(1)) },
                            onNext = { if (month.isBefore(currentMonth)) draft = draft.copy(date = draft.date.plusMonths(1)) }
                        )
                    }
                    Period.YEAR -> {
                        PresetRow(
                            first = "This Year",
                            firstSelected = draft.date.year == today.year,
                            second = "Last Year",
                            secondSelected = draft.date.year == today.year - 1,
                            onFirst = { draft = draft.copy(date = today) },
                            onSecond = { draft = draft.copy(date = today.minusYears(1)) }
                        )
                        DateStepper(
                            label = draft.date.year.toString(),
                            canNext = draft.date.year < today.year,
                            onPrevious = { draft = draft.copy(date = draft.date.minusYears(1)) },
                            onNext = { if (draft.date.year < today.year) draft = draft.copy(date = draft.date.plusYears(1)) }
                        )
                    }
                    Period.CUSTOM -> {
                        val range = draft.customRange ?: DateRange(today, today)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(modifier = Modifier.weight(1f), onClick = { datePickerTarget = "start" }) {
                                Text("From: ${range.start.format(shortDateFormatter)}")
                            }
                            OutlinedButton(modifier = Modifier.weight(1f), onClick = { datePickerTarget = "end" }) {
                                Text("To: ${range.end.format(shortDateFormatter)}")
                            }
                        }
                        if (range.start.isAfter(range.end)) {
                            Text("Start date must be on or before end date.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            FilterPanel(title = "AMOUNT BRACKET") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    AmountChip("ALL", draft.minimumPaise == null && draft.maximumPaise == null) {
                        draft = draft.copy(minimumPaise = null, maximumPaise = null)
                        minimumText = ""; maximumText = ""
                    }
                    AmountChip("< ₹500", draft.minimumPaise == null && draft.maximumPaise == 49_999L) {
                        draft = draft.copy(minimumPaise = null, maximumPaise = 49_999L)
                        minimumText = ""; maximumText = ""
                    }
                    AmountChip("₹500–₹1,999", draft.minimumPaise == 50_000L && draft.maximumPaise == 199_999L) {
                        draft = draft.copy(minimumPaise = 50_000L, maximumPaise = 199_999L)
                        minimumText = ""; maximumText = ""
                    }
                    AmountChip("₹2,000+", draft.minimumPaise == 200_000L && draft.maximumPaise == null) {
                        draft = draft.copy(minimumPaise = 200_000L, maximumPaise = null)
                        minimumText = ""; maximumText = ""
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minimumText,
                        onValueChange = {
                            minimumText = it
                            validationMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Min ₹") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = maximumText,
                        onValueChange = {
                            maximumText = it
                            validationMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Max ₹") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            FilterPanel(title = "TRANSACTION TYPE") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("all" to "ALL", "debit" to "DEBIT", "credit" to "CREDIT").forEach { (value, label) ->
                        FilterChip(selected = draft.direction == value, onClick = { draft = draft.copy(direction = value) }, label = { Text(label) })
                    }
                }
            }
            FilterPanel(title = "CATEGORIES") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(
                        selected = draft.categories.isEmpty(),
                        onClick = { draft = draft.copy(categories = emptySet()) },
                        label = { Text("ALL") }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id in draft.categories,
                            onClick = {
                                val next = draft.categories.toMutableSet()
                                if (!next.add(category.id)) next.remove(category.id)
                                draft = draft.copy(categories = next)
                            },
                            label = { Text("${category.icon} ${category.name}") }
                        )
                    }
                }
            }
            }
                    (filterValidationMessage ?: validationMessage)?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                draft = TransactionFilters(date = today)
                                minimumText = ""
                                maximumText = ""
                                validationMessage = null
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .heightIn(min = 52.dp)
                                .semantics { contentDescription = "Reset all filter selections" }
                        ) {
                            Text("Reset")
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text("Cancel", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                validationMessage = when {
                                    minimumText.isNotBlank() && minValue == null -> "Enter a valid minimum amount."
                                    maximumText.isNotBlank() && maxValue == null -> "Enter a valid maximum amount."
                                    minValue != null && maxValue != null && minValue > maxValue ->
                                        "Minimum amount must not exceed maximum."
                                    draft.period == Period.CUSTOM && !canApply ->
                                        "Select a valid date range up to today."
                                    else -> null
                                }
                                if (canApply) {
                                    val typedAmount = minimumText.isNotBlank() || maximumText.isNotBlank()
                                    onApply(
                                        draft.copy(
                                            minimumPaise = if (typedAmount) minValue else draft.minimumPaise,
                                            maximumPaise = if (typedAmount) maxValue else draft.maximumPaise
                                        )
                                    )
                                }
                            },
                            enabled = canApply,
                            modifier = Modifier
                                .weight(1.45f)
                                .heightIn(min = 52.dp)
                                .semantics { contentDescription = "Apply filters and show matching transactions" },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFB8C0CC),
                                disabledContentColor = Color(0xFF5C6675)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            Text("Show results", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    datePickerTarget?.let { target ->
        val range = draft.customRange ?: DateRange(today, today)
        val initialDate = if (target == "start") range.start else range.end
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toUtcMillis(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= today.toUtcMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = millis.toLocalDateUtc()
                        draft = if (target == "start") {
                            draft.copy(customRange = DateRange(selected, range.end))
                        } else {
                            draft.copy(customRange = DateRange(range.start, selected))
                        }
                    }
                    datePickerTarget = null
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { datePickerTarget = null }) { Text("Cancel") }
            }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun FilterPanel(title: String, content: @Composable () -> Unit) {
    val panelBackground = if (MaterialTheme.colorScheme.background == Color(0xFF121212)) {
        Color(0xFF273449)
    } else {
        Color(0xFFE9EDF3)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(10.dp))
                .background(panelBackground).padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, letterSpacing = 0.7.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun PresetRow(
    first: String,
    firstSelected: Boolean,
    second: String,
    secondSelected: Boolean,
    onFirst: () -> Unit,
    onSecond: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = firstSelected, onClick = onFirst, label = { Text(first) })
        FilterChip(selected = secondSelected, onClick = onSecond, label = { Text(second) })
    }
}

@Composable
private fun DateStepper(label: String, canNext: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPrevious) { Text("‹") }
        Text(
            label,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
        OutlinedButton(onClick = onNext, enabled = canNext) { Text("›") }
    }
}

@Composable
private fun AmountChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}
