package com.personalexpensetracker.data.notification.detection

/**
 * Detects whether a notification is likely a financial transaction notification.
 *
 * Uses keyword matching against common banking, UPI, card, and wallet
 * notification patterns. Designed to be extensible — new patterns can be
 * added without restructuring.
 *
 * This is a LOCAL-ONLY detector. No data is sent to external services.
 */
object FinancialNotificationDetector {

    /**
     * Keywords that strongly indicate a financial transaction notification.
     * Case-insensitive matching is used.
     */
    private val FINANCIAL_KEYWORDS = listOf(
        // Debit indicators
        "debited", "debit", "spent", "paid", "payment",
        "purchase", "transaction", "withdrawn", "withdrawal",
        "amount deducted", "amount paid", "payment successful",
        "payment made", "money sent", "sent", "transferred", "transfer",
        // UPI-specific
        "upi payment", "upi transaction", "upi transfer", "upi",
        // Card-specific
        "card payment", "card purchase", "card transaction",
        "charged", "pos transaction",
        // Credit indicators (still financial — classification happens later)
        "credited", "credit", "received", "deposit",
        "money received", "salary credited", "refund",
        "cashback", "amount credited",
        // General transaction terms
        "a/c", "account", "acct", "bal", "balance",
        "txn", "ref no", "ref:", "reference"
    )

    /**
     * Patterns that indicate the notification is NOT a financial transaction
     * even if it contains some financial-sounding words.
     */
    private val EXCLUSION_PATTERNS = listOf(
        "otp", "one time password", "verification code",
        "login", "sign in", "password reset",
        "your code is", "code:", "pin:",
        "offer", "cashback offer", "discount offer",
        "download", "update available", "app update",
        "rate us", "review", "feedback",
        "happy birthday", "wish", "greeting"
    )

    /**
     * Amount-related patterns that help confirm financial context.
     * These look for currency symbols or currency words near numbers.
     */
    private val AMOUNT_INDICATORS = listOf(
        "₹", "rs", "rs.", "inr", "usd", "\\$"
    )

    /**
     * Determines whether the given notification text is likely a financial
     * transaction notification.
     *
     * @param combinedText All available text from the notification concatenated.
     * @return true if the notification appears to be financial.
     */
    fun isFinancialNotification(combinedText: String): Boolean {
        if (combinedText.isBlank()) return false

        val lowerText = combinedText.lowercase()

        // Check exclusion patterns first — quick rejection
        if (EXCLUSION_PATTERNS.any { lowerText.contains(it) }) {
            // OTP/login/code notifications must never be treated as completed transactions
            if (lowerText.contains("otp") || lowerText.contains("verification code") ||
                lowerText.contains("your code is") || lowerText.contains("login pin") ||
                lowerText.contains("password reset")
            ) {
                return false
            }

            val hasStrongFinancialSignal = hasStrongFinancialSignal(lowerText)
            if (!hasStrongFinancialSignal) return false
        }

        // Check for financial keywords
        val hasFinancialKeyword = FINANCIAL_KEYWORDS.any { lowerText.contains(it) }

        // Check for amount indicators (currency symbols/words)
        val hasAmountIndicator = AMOUNT_INDICATORS.any { lowerText.contains(it) }

        // Strong signal: both a financial keyword AND an amount indicator
        if (hasFinancialKeyword && hasAmountIndicator) return true

        // Moderate signal: financial keyword alone with number present
        if (hasFinancialKeyword && containsNumber(lowerText)) return true

        return false
    }

    /**
     * Checks whether the text contains strong financial signals that can override
     * weak exclusion patterns (e.g. promotional text mentioning an actual debit).
     */
    private fun hasStrongFinancialSignal(lowerText: String): Boolean {
        val hasDebit = lowerText.contains("debited") || lowerText.contains("amount deducted")
        val hasCredit = lowerText.contains("credited") || lowerText.contains("salary credited")
        val hasCurrency = AMOUNT_INDICATORS.any { lowerText.contains(it) }
        return (hasDebit || hasCredit) && hasCurrency && containsNumber(lowerText)
    }

    /**
     * Checks if the text contains any digit.
     */
    private fun containsNumber(text: String): Boolean {
        return text.any { it.isDigit() }
    }
}
