package com.example.smsexpense.ui

import androidx.compose.material3.Card
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

internal fun TransactionFilters.matchesDate(date: LocalDate, today: LocalDate): Boolean {
    if (date.isAfter(today)) return false
    return when (period) {
        Period.DAY -> date == this.date
        Period.WEEK -> date.startOfWeek() == this.date.startOfWeek() &&
            (weekDay == null || date == weekDay)
        Period.MONTH -> date.year == this.date.year && date.month == this.date.month
        Period.YEAR -> date.year == this.date.year
        Period.CUSTOM -> customRange?.let { date >= it.start && date <= it.end } ?: false
    }
}

internal fun TransactionFilters.activeCount(today: LocalDate): Int = listOf(
    period != Period.DAY || date != today,
    categories.isNotEmpty(),
    minimumPaise != null || maximumPaise != null,
    direction != "all"
).count { it }

internal fun TransactionFilters.heading(today: LocalDate): String = when (period) {
    Period.DAY -> if (date == today) "Today's Transactions" else "${date.format(shortDateFormatter)} Transactions"
    Period.WEEK -> if (date.startOfWeek() == today.startOfWeek()) "This Week's Transactions" else "Week of ${date.startOfWeek().format(shortDateFormatter)}"
    Period.MONTH -> if (date.year == today.year && date.month == today.month) "This Month's Transactions" else date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    Period.YEAR -> if (date.year == today.year) "This Year's Transactions" else "${date.year} Transactions"
    Period.CUSTOM -> "Custom Date Range"
}

internal fun TransactionFilters.dateLabel(today: LocalDate): String = when (period) {
    Period.DAY -> when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(shortDateFormatter)
    }
    Period.WEEK -> "${date.startOfWeek().format(shortDateFormatter)} – ${minOf(date.startOfWeek().plusDays(6), today).format(shortDateFormatter)}"
    Period.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    Period.YEAR -> date.year.toString()
    Period.CUSTOM -> customRange?.let { "${it.start.format(shortDateFormatter)} – ${it.end.format(shortDateFormatter)}" } ?: "Choose dates"
}

internal fun TransactionFilters.aggregateTitle(today: LocalDate): String = when (period) {
    Period.DAY -> if (date == today) "ON-DEVICE DAY AGGREGATE" else "ON-DEVICE DAILY AGGREGATE"
    Period.WEEK -> "ON-DEVICE WEEK AGGREGATE"
    Period.MONTH -> "ON-DEVICE MONTH AGGREGATE"
    Period.YEAR -> "ON-DEVICE YEAR AGGREGATE"
    Period.CUSTOM -> "ON-DEVICE DATE AGGREGATE"
}

internal fun LocalDate.startOfWeek(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

internal fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

internal fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

internal fun String.detectChannel(): String? {
    val text = lowercase(Locale.ROOT)
    return when {
        "upi" in text -> "UPI"
        "neft" in text -> "NEFT"
        "imps" in text -> "IMPS"
        "rtgs" in text -> "RTGS"
        "atm" in text -> "ATM"
        "pos" in text -> "POS"
        "card" in text -> "Card"
        else -> null
    }
}

internal fun String.detectReference(): String? {
    val pattern = Regex(
        """(?i)\b(?:UPI\s*Ref(?:erence)?(?:\s*(?:No\.?|Number))?|Ref(?:erence)?(?:\s*(?:No\.?|Number))?|UTR|Txn(?:\s*ID)?)\s*[:#-]?\s*([A-Z0-9-]{5,})"""
    )
    return pattern.find(this)?.groupValues?.getOrNull(1)
}

internal fun parsePaise(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        BigDecimal(value.trim()).movePointRight(2).longValueExact().takeIf { it >= 0 }
    } catch (_: NumberFormatException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

internal fun paiseToInput(paise: Long): String =
    BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString()

internal fun formatMoney(paise: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    formatter.currency = java.util.Currency.getInstance("INR")
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.format(paise / 100.0)
}

internal fun signedMoney(paise: Long): String = when {
    paise > 0 -> "+${formatMoney(paise)}"
    paise < 0 -> "−${formatMoney(kotlin.math.abs(paise))}"
    else -> formatMoney(0)
}
