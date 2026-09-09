package com.personalexpensetracker.data.notification.parser

import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier
import com.personalexpensetracker.data.notification.model.RawNotificationData
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parses financial transaction notifications to extract structured data.
 *
 * Extracts:
 * - Amount (supports ₹, Rs, Rs., INR formats with commas, decimals, suffix currency,
 *   currency-less amounts following debit verbs, and balance-aware candidate scoring)
 * - Merchant/payee name (supports UPI VPAs with @ and /, rejects account numbers,
 *   filters out amount phrases, and falls back to notification title if merchant is not found in body)
 * - Transaction reference ID
 *
 * ## Safety Rules
 * - If amount cannot be confidently extracted, returns null (no expense created).
 * - Balance amounts (Avl Bal, Available Balance, Closing Balance, Credit Limit)
 *   are NEVER extracted as transaction amounts.
 * - Account numbers/cards ("from A/c XXXX", "Account 1234") are NEVER treated as merchants.
 * - If merchant cannot be extracted, uses a safe fallback ("Unknown Merchant").
 * - Numbers that don't look like transaction amounts (OTPs, PINs, account numbers)
 *   are filtered out using contextual validation.
 *
 * ## Design
 * - Modular: source-specific parsing rules can be added via [SourceAwareParser].
 * - Extensible: regex patterns are organized for easy maintenance.
 * - Local-only: no external API calls.
 */
object TransactionParser {

    /**
     * Regex to match Indian currency amounts with prefix:
     * ₹450, Rs 450, Rs. 450, INR 450, ₹1,250.50, Rs.1,250, etc.
     */
    private val AMOUNT_PREFIX_REGEX = Regex(
        """(?:₹|[Rr][Ss]\.?\s*|INR\s*)\s*((?:\d{1,3}(?:,\d{2,3})*|\d+)(?:\.\d{1,2})?)(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Fallback: amount followed by currency indicator.
     * E.g., "1,250 INR", "450 rupees", "500 rs"
     */
    private val AMOUNT_SUFFIX_REGEX = Regex(
        """((?:\d{1,3}(?:,\d{2,3})*|\d+)(?:\.\d{1,2})?)\s*(?:₹|[Rr][Ss]\.?|INR|rupees?)(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Currency-less amount directly following debit action verbs.
     * E.g., "debited by 450.00", "debited for 450", "spent 1200", "paid 350.50"
     */
    private val AMOUNT_DEBIT_VERB_REGEX = Regex(
        """(?:debited\s*(?:by|for|of|with)?|spent|paid|withdrawn\s*(?:by|for|of)?)\s*[:\s]+((?:\d{1,3}(?:,\d{2,3})*|\d+)(?:\.\d{1,2})?)(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Context prefixes that indicate an amount is an account balance or limit,
     * NOT a debited transaction amount.
     */
    private val BALANCE_PREFIX_REGEX = Regex(
        """(?:avl\s*bal|available\s*bal(?:ance)?|tot\s*bal|total\s*bal(?:ance)?|closing\s*bal(?:ance)?|clear\s*bal(?:ance)?|ledger\s*bal(?:ance)?|ac\s*bal(?:ance)?|rem\s*bal(?:ance)?|bal(?:ance)?[:\s]|limit[:\s]|credit\s*limit[:\s]|avl\s*lmt[:\s])""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Debit action words that provide strong evidence that an amount
     * is the actual debited transaction amount.
     */
    private val DEBIT_ACTION_REGEX = Regex(
        """(?:debited\s*(?:for|by|of|with)?|spent|paid|purchase\s*(?:of|for)?|deducted|withdrawn\s*(?:by|of|for)?|sent|transferred|charge\s*of|payment\s*(?:of|for)|txn\s*of)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Patterns to extract merchant names.
     * Captures merchant / payee / VPA name in group 1.
     * Lookahead stops at keywords, sentence boundaries (.\s or .$), or end of string (\s*$).
     */
    private val MERCHANT_PATTERNS = listOf(
        // Explicit payee/sender indicators
        Regex("""(?:to|towards|paid to|payment to|sent to|transferred to|received from|from|vpa[:\s]|payee[:\s]|merchant[:\s]|info[:\s])\s+(?!(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*))([A-Za-z0-9][A-Za-z0-9\s.&'@/-]{0,50}?)(?=(?:\s+(?:for|of|on|via|using|with|was|has|ref|txn|w\.e\.f|avl|bal|available|balance|successfully|successful|sms)|\s*$|\.\s|\.$))""", RegexOption.IGNORE_CASE),
        Regex("""(?:to|towards|paid to|payment to|sent to|transferred to|received from|from|vpa[:\s]|payee[:\s]|merchant[:\s]|info[:\s])\s+(?!(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*))([A-Za-z0-9][A-Za-z0-9\s.&'@/-]{0,50}?)(?:\s*[.\-])?\s*$""", RegexOption.IGNORE_CASE),
        // for / at / purchase at / purchase from / order at
        Regex("""(?:for|at|purchase at|purchase from|order at)\s+(?!(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*))([A-Za-z0-9][A-Za-z0-9\s.&'@/-]{0,50}?)(?=(?:\s+(?:for|of|on|via|using|with|was|has|ref|txn|w\.e\.f|avl|bal|available|balance|successfully|successful|sms)|\s*$|\.\s|\.$))""", RegexOption.IGNORE_CASE),
        Regex("""(?:for|at|purchase at|purchase from|order at)\s+(?!(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*))([A-Za-z0-9][A-Za-z0-9\s.&'@/-]{0,50}?)(?:\s*[.\-])?\s*$""", RegexOption.IGNORE_CASE),
        // Fallback simple
        Regex("""(?:for|to|at|towards)\s+(?!(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*))([A-Za-z][A-Za-z0-9\s.&'@/-]{1,40})""", RegexOption.IGNORE_CASE)
    )

    /**
     * Patterns to extract reference/transaction IDs.
     */
    private val REFERENCE_PATTERNS = listOf(
        Regex("""(?:ref\.?\s*(?:no\.?\s*)?|reference\s*(?:no\.?\s*)?|txn\s*(?:id\s*)?|transaction\s*(?:id\s*)?)[:\s]*([A-Za-z0-9/-]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:upi\s*ref\s*(?:no\.?\s*)?)[:\s]*(\d+)""", RegexOption.IGNORE_CASE)
    )

    /**
     * Patterns to extract account or card trailing digits (3-4 digits).
     */
    private val ACCOUNT_LAST4_PATTERNS = listOf(
        Regex("""(?:a/?c|acct|account|card)\s*(?:no\.?|number)?\s*(?:ending\s*(?:in|with)?|is)\s*[*xX]*(\d{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\bending\s*(?:in|with)?\s*[*xX]*(\d{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""(?:a/?c|acct|account|card)\s*(?:no\.?|number)?[:\s]*[*xX]{2,}(\d{3,4})\b""", RegexOption.IGNORE_CASE),
        Regex("""(?:\b|[*xX])[*xX]{2,}(\d{3,4})\b"""),
        Regex("""\b(?:a/?c|acct|account|card)\s*(?:no\.?|number)?[:\s]+(\d{4})\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Keywords that identify a notification title as a generic bank or system alert
     * rather than an actual merchant name.
     */
    private val SYSTEM_OR_BANK_TITLE_KEYWORDS = listOf(
        "bank", "banking", "alert", "alerts", "notification", "finly", "messages",
        "sms", "phonepe", "gpay", "google pay", "paytm", "bhim", "cred", "sbi",
        "hdfc", "icici", "axis", "kotak", "pnb", "bob", "canara", "union", "idfc"
    )

    /**
     * Attempts to parse a financial transaction from notification data.
     *
     * @param notification The raw notification data.
     * @param direction The pre-classified transaction direction.
     * @return A [ParsedTransaction] if parsing succeeds, or null if the
     *         amount cannot be confidently extracted.
     */
    fun parse(
        notification: RawNotificationData,
        direction: TransactionDirectionClassifier.TransactionDirection
    ): ParsedTransaction? {
        val contextText = notification.combinedText.ifBlank { notification.fullText ?: "" }
        if (contextText.isBlank()) return null

        // Extract amount — this is REQUIRED
        val amount = extractAmount(contextText) ?: return null

        // Validate amount is reasonable (not zero, not absurdly large)
        if (amount <= BigDecimal.ZERO || amount > BigDecimal("99999999.99")) return null

        // Extract merchant from combined text first
        var merchant = extractMerchant(contextText)

        // If merchant not found, check if the notification title is a merchant name
        if (merchant == null) {
            val title = notification.title?.trim()
            if (!title.isNullOrBlank() && !isAccountPhrase(title) && !isSystemOrBankTitle(title) && !isAmountOrCurrencyPhrase(title)) {
                merchant = cleanMerchant(title)
            }
        }

        // Extract reference ID — optional
        val referenceId = extractReferenceId(contextText)

        // Extract account/card trailing digits for cross-channel deduplication
        val accountLast4 = extractAccountLast4(contextText)

        val defaultMerchant = if (direction == TransactionDirectionClassifier.TransactionDirection.CREDIT) "Income" else "Unknown Merchant"

        return ParsedTransaction(
            amount = amount,
            merchant = merchant ?: defaultMerchant,
            direction = direction,
            transactionTime = notification.receivedAt,
            sourcePackage = notification.packageName,
            referenceId = referenceId,
            notificationKey = notification.notificationKey,
            accountLast4 = accountLast4
        )
    }

    /**
     * Extracts a monetary amount from the notification text.
     * Balance-aware: ignores amounts that appear in "Avl Bal", "Balance", etc.
     * Returns null if no valid debit amount can be confidently identified.
     */
    internal fun extractAmount(text: String): BigDecimal? {
        if (isOtpOrPinOnly(text)) {
            return null
        }

        data class AmountCandidate(
            val amount: BigDecimal,
            val startIndex: Int,
            val endIndex: Int,
            val isBalance: Boolean,
            val isDebitAction: Boolean
        )

        val candidates = mutableListOf<AmountCandidate>()

        fun evaluateCandidate(amount: BigDecimal, start: Int, end: Int) {
            val preceding = text.substring((start - 40).coerceAtLeast(0), start)
            val sentenceStart = preceding.lastIndexOfAny(charArrayOf('.', ';', '\n', '|')).let { if (it >= 0) it + 1 else 0 }
            val localPreceding = preceding.substring(sentenceStart)

            val following = text.substring(end + 1, (end + 30).coerceAtMost(text.length))
            val sentenceEnd = following.indexOfAny(charArrayOf('.', ';', '\n', '|')).let { if (it >= 0) it else following.length }
            val localFollowing = following.substring(0, sentenceEnd)

            val isBalance = BALANCE_PREFIX_REGEX.containsMatchIn(localPreceding)
            val isDebit = DEBIT_ACTION_REGEX.containsMatchIn(localPreceding) || DEBIT_ACTION_REGEX.containsMatchIn(localFollowing)

            candidates.add(AmountCandidate(amount, start, end, isBalance, isDebit))
        }

        // 1. Check currency prefix matches: ₹450, Rs 450, INR 450
        for (match in AMOUNT_PREFIX_REGEX.findAll(text)) {
            val amount = parseAmountString(match.groupValues[1]) ?: continue
            evaluateCandidate(amount, match.range.first, match.range.last)
        }

        // 2. Check currency suffix matches: 1,250 INR, 500 rupees
        for (match in AMOUNT_SUFFIX_REGEX.findAll(text)) {
            val amount = parseAmountString(match.groupValues[1]) ?: continue
            val start = match.range.first
            if (candidates.any { it.startIndex == start }) continue
            evaluateCandidate(amount, start, match.range.last)
        }

        // 3. Check currency-less debit verbs: "debited by 450.00", "spent 1200"
        for (match in AMOUNT_DEBIT_VERB_REGEX.findAll(text)) {
            val amount = parseAmountString(match.groupValues[1]) ?: continue
            val start = match.range.first
            if (candidates.none { it.amount.compareTo(amount) == 0 && Math.abs(it.startIndex - start) < 20 }) {
                candidates.add(AmountCandidate(amount, start, match.range.last, isBalance = false, isDebitAction = true))
            }
        }

        if (candidates.isEmpty()) return null

        val nonBalanceCandidates = candidates.filter { !it.isBalance }
        if (nonBalanceCandidates.isEmpty()) return null

        val debitCandidate = nonBalanceCandidates.firstOrNull { it.isDebitAction }
        if (debitCandidate != null) {
            return debitCandidate.amount
        }

        return nonBalanceCandidates.first().amount
    }

    private fun parseAmountString(amountStr: String): BigDecimal? {
        return try {
            val cleaned = amountStr.replace(",", "")
            val amount = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            if (amount > BigDecimal.ZERO) amount else null
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Extracts merchant/payee name from notification text.
     * Rejects user bank account phrases, amount phrases, and trailing status words.
     * Returns null if no valid merchant can be identified.
     */
    internal fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            for (match in pattern.findAll(text)) {
                val candidate = cleanMerchant(match.groupValues[1])
                if (candidate.length >= 2 && !isAccountPhrase(candidate) && !isAmountOrCurrencyPhrase(candidate)) {
                    return candidate
                }
            }
        }
        return null
    }

    /**
     * Cleans and normalizes candidate merchant strings.
     */
    private fun cleanMerchant(raw: String): String {
        var s = raw.trim()

        // Truncate at sentence boundary (period followed by space)
        val dotSpaceIndex = s.indexOf(". ")
        if (dotSpaceIndex >= 0) {
            s = s.substring(0, dotSpaceIndex).trim()
        }

        // Clean trailing punctuation
        s = s.trimEnd('.', '-', ',', ' ', ':')

        // Clean trailing status or noise words
        val noiseWords = listOf("successfully", "successful", "completed", "approved", "done", "available", "balance", "sms")
        for (word in noiseWords) {
            if (s.lowercase().endsWith(word)) {
                s = s.substring(0, s.length - word.length).trimEnd('.', '-', ',', ' ')
            }
        }

        // If candidate is a UPI VPA (contains @), extract just the VPA handle (no following words)
        if (s.contains("@")) {
            s = s.split(Regex("""\s+""")).first().trim()
        }

        return s
    }

    private fun isAccountPhrase(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.startsWith("a/c") ||
            lower.startsWith("acct") ||
            lower.startsWith("account") ||
            lower.startsWith("card") ||
            lower.startsWith("my card") ||
            lower.contains("ending") ||
            lower.contains("xxxx") ||
            lower.contains("xx") ||
            lower.contains("**") ||
            lower.matches(Regex("""^(?:a/?c|account|acct|card)?\s*[*xX\d\s-]+$"""))) {
            return true
        }
        if (lower.matches(Regex("""^[\d\s./-]+$"""))) return true
        return false
    }

    private fun isAmountOrCurrencyPhrase(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.startsWith("inr") || lower.startsWith("rs") || lower.startsWith("₹") || lower.startsWith("$")) {
            return true
        }
        if (lower.endsWith("inr") || lower.endsWith("rupees") || lower.endsWith("rs")) {
            return true
        }
        if (lower.matches(Regex("""^[\d\s.,]+$"""))) {
            return true
        }
        return false
    }

    private fun isSystemOrBankTitle(title: String): Boolean {
        val lower = title.lowercase().trim()
        return SYSTEM_OR_BANK_TITLE_KEYWORDS.any { lower.contains(it) }
    }

    private fun isOtpOrPinOnly(text: String): Boolean {
        val hasOtp = Regex("""\b(?:otp|one time password|pin|verification code|secret code)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
        val hasFinancialAction = Regex("""\b(?:debited|credited|spent|paid|purchase|transfer|withdrawn)\b""", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
        return hasOtp && !hasFinancialAction
    }

    internal fun extractReferenceId(text: String): String? {
        for (pattern in REFERENCE_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val refId = match.groupValues[1].trim()
                if (refId.isNotBlank() && refId.length >= 3) {
                    return refId
                }
            }
        }
        return null
    }

    internal fun extractAccountLast4(text: String): String? {
        for (pattern in ACCOUNT_LAST4_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val last4 = match.groupValues[1].trim()
                if (last4.isNotBlank() && last4.length in 3..4) {
                    return last4
                }
            }
        }
        return null
    }
}
