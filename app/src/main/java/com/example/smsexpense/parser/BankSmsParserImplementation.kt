package com.example.smsexpense.parser

import java.util.regex.Pattern

abstract class BankSmsParser {
    data class Classification(val isBank: Boolean, val reason: String?, val score: Int)
    abstract fun classify(body: String, sender: String, timestamp: Long): Classification
    abstract fun extractAmount(text: String): Long?
    abstract fun extractDirection(text: String): String
    abstract fun extractMerchant(text: String): String?
    abstract fun extractAccountLast4(text: String): String?
    abstract fun extractCounterparty(text: String): String?
    abstract fun extractUpiReference(text: String): String?
    abstract fun extractBalanceAfter(text: String): Long?
}

class BankSmsParserImplementation : BankSmsParser() {
    private fun looksLikeBankId(sender: String): Boolean {
        val hasLetters = sender.any { it.isLetter() }
        val hasDigits = sender.any { it.isDigit() }
        val notPureDigits = !sender.all { it.isDigit() } || sender.length !in 9..15
        val reasonablyLong = sender.length >= 3
        return hasLetters && hasDigits && notPureDigits && reasonablyLong
    }
    private fun hasAmountPattern(text: String): Boolean {
        val p = Pattern.compile("(?:rs\\.?|inr\\.?|\\$|₹)\\s*[0-9][0-9,]*(?:\\.[0-9]+)?", Pattern.CASE_INSENSITIVE)
        return p.matcher(text).find()
    }
    private fun hasAvailableBalance(text: String): Boolean =
        text.contains("avl bal") || text.contains("available balance") || text.contains("bal:")

    override fun classify(body: String, sender: String, timestamp: Long): Classification {
        val text = body.lowercase()
        val rejectedKeywords = listOf("otp","one time password","verification code","do not share","offer","cashback offer","loan offer","apply now","click here","unsubscribe","bill is due","will be debited","request")
        for (kw in rejectedKeywords) if (text.contains(kw)) return Classification(false, "rejected: $kw", 0)
        var score = 0
        if (looksLikeBankId(sender)) score += 2
        if (hasAmountPattern(text)) score += 3
        val transactionKeywords = listOf("debited","credited","spent","paid","withdrawn","purchase","upi","neft","imps","atm","txn","transaction","a/c","acct","card")
        var kwCount = 0
        for (kw in transactionKeywords) if (text.contains(kw)) { kwCount++; if (kwCount <= 4) score += 2 }
        if (hasAvailableBalance(text)) score += 1
        return if (score >= 5) Classification(true, null, score) else Classification(false, "score=$score < 5", score)
    }

    override fun extractAmount(text: String): Long? {
        val p = Pattern.compile("(?:rs\\.?|inr\\.?|₹|\\$)\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(text)
        if (m.find()) {
            val num = m.group(1).replace(",", "")
            return try {
                java.math.BigDecimal(num)
                    .setScale(2, java.math.RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact()
            } catch (_: NumberFormatException) {
                null
            } catch (_: ArithmeticException) {
                null
            }
        }
        return null
    }

    override fun extractDirection(text: String): String {
        val t = text.lowercase()
        return if (t.contains("debited") || t.contains("spent") || t.contains("withdrawn")) "DEBIT" else "CREDIT"
    }

    override fun extractMerchant(text: String): String? {
        val p = Pattern.compile("\\bat\\b\\s+([^\\.\\n]{1,50})", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(text)
        while (m.find()) {
            val candidate = m.group(1).trim()
            if (!candidate.lowercase().matches(Regex(".*(on|ref|upi|transaction).*"))) return candidate.take(30)
        }
        return null
    }

    override fun extractAccountLast4(text: String): String? {
        val patterns = listOf(
            "(?:a/c|account|acct|card)\\s*(?:(?:no\\.?|number)\\s*)?[:#-]?\\s*[xX*•\\s-]*([0-9]{3,})",
            "(?:a/c|account|acct|card)\\s*(?:ending|ends)\\s*(?:in|with)?\\s*[:#-]?\\s*[xX*•\\s-]*([0-9]{3,})",
            "\\b(?:xx|\\*{2,}|•{2,})\\s*([0-9]{4})\\b"
        )
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text)
            if (matcher.find()) return matcher.group(1).takeLast(4)
        }
        return null
    }

    override fun extractCounterparty(text: String): String? {
        val patterns = listOf(
            "(?i)\\b(?:paid|sent|transferred)\\s+to\\s+([^,;\\n.]+)",
            "(?i)\\b(?:received|credited)\\s+(?:from|by)\\s+([^,;\\n.]+)",
            "(?i)\\b([A-Z][A-Z .'-]{2,40}?)\\s+credited\\b",
            "(?i)\\bto\\s+(?:vpa\\s*[:/-]?\\s*)?([^,;\\n]+)",
            "(?i)\\bfrom\\s+([A-Z][A-Z .'-]{2,40})(?=\\s+(?:via|using|on|ref|upi|utr|txn|transaction)\\b|[,;.\\n]|$)"
        )
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)
                    .split(
                        Regex(
                            "\\s+(?:from\\s+(?:a/c|account)|via|using|on|upi|vpa|ref|utr|txn|transaction)\\b",
                            RegexOption.IGNORE_CASE
                        )
                    )
                    .first()
                    .trim()
                    .trim('-', ':', ' ')
                val cleaned = candidate
                    .removePrefix("VPA:")
                    .removePrefix("vpa:")
                    .trim()
                    .substringBefore('@')
                    .trim()
                if (cleaned.isNotBlank() &&
                    !cleaned.matches(Regex("(?i).*(?:a/c|account|xx\\d+|\\*\\d+|\\d{6,}).*"))
                ) return cleaned.take(50)
            }
        }
        return null
    }

    override fun extractUpiReference(text: String): String? {
        val patterns = listOf(
            "(?i)\\b(?:UPI\\s*)?(?:ref(?:erence)?|UTR|txn(?:\\s*id)?)(?:\\s*(?:no\\.?|number))?\\s*[:#-]?\\s*([A-Z0-9-]{6,})",
            "(?i)\\bUPI\\s*[:#-]\\s*([A-Z0-9-]{6,})"
        )
        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(text)
            if (matcher.find()) return matcher.group(1)?.trimEnd('.', ',')
        }
        return null
    }

    override fun extractBalanceAfter(text: String): Long? {
        val p = Pattern.compile("(?:avl[\\s]*bal|available[\\s]*bal|bal:)\\s*₹?\\s*([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(text.lowercase())
        if (m.find()) {
            val numStr = m.group(1).replace(",", "")
            val parts = numStr.split(".")
            val rupees = parts[0].toLongOrNull() ?: 0L
            val paise = if (parts.size > 1) parts[1].padEnd(2,'0').take(2).toLongOrNull() ?: 0L else 0L
            return rupees * 100 + paise
        }
        return null
    }
}
