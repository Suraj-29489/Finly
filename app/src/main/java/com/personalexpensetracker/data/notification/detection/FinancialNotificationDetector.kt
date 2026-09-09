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
        "purchase", "transaction", "withdrawn", "withdrawal", "atm withdrawal",
        "amount deducted", "amount paid", "payment successful",
        "payment made", "money sent", "sent", "transferred", "transfer",
        "charged", "deducted",
        // Indian bank Dr indicators
        "dr.", "dr for", "dr by", "dr with", "is dr",
        // UPI-specific
        "upi payment", "upi transaction", "upi transfer", "upi",
        // Card-specific
        "card payment", "card purchase", "card transaction",
        "pos transaction", "pos txn",
        // Credit indicators (still financial — classification happens later)
        "credited", "credit", "received", "deposit",
        "money received", "salary credited", "refund",
        "cashback", "amount credited",
        // General transaction terms
        "a/c", "account", "acct", "bal", "balance",
        "txn", "ref no", "ref:", "reference",
        // Common payment apps
        "google pay", "gpay", "phonepe", "paytm", "bhim", "cred", "amazon pay"
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

    private val NEGATIVE_AMOUNT_DETECTOR_REGEX = Regex(
        """(?:^|[\s:;(\[|])[-−–—]\s*(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*)?\d+"""
    )

    /**
     * Checks if text contains a negative amount like -1, -500, -₹500, -Rs 100
     */
    fun hasNegativeAmount(text: String): Boolean {
        return NEGATIVE_AMOUNT_DETECTOR_REGEX.containsMatchIn(text)
    }

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

        // Check for amount indicators (currency symbols/words or negative amounts)
        val hasAmountIndicator = AMOUNT_INDICATORS.any { lowerText.contains(it) } || hasNegativeAmount(lowerText)

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

    /**
     * Regex matching Indian TRAI-regulated commercial / transactional bank SMS sender headers:
     * Examples: VK-HDFCBK, VM-SBIINB, AD-ICICIB, BZ-AXISBK, AX-KOTAKB, JM-PAYTMB
     * Format: 2 alpha characters, optional hyphen, 5 to 9 alphanumeric characters.
     */
    private val TRAI_BANK_HEADER_REGEX = Regex(
        """^[A-Za-z]{2}-?[A-Za-z0-9]{5,9}$"""
    )

    /**
     * Common bank and financial entity keywords in sender titles.
     */
    private val BANK_SENDER_KEYWORDS = listOf(
        "bank", "banking", "hdfc", "sbi", "icici", "axis", "kotak", "pnb",
        "bob", "canara", "union", "idfc", "rbl", "yes bank", "yesbnk",
        "indusind", "federal", "hsbc", "citi", "standard chartered", "scb",
        "cred", "paytm", "phonepe", "gpay", "google pay", "slice", "onecard",
        "jupiter", "fi money", "bhim", "amazon pay", "mobikwik"
    )

    /**
     * Contextual indicators that confirm the text describes an actual bank/card/account transaction.
     */
    private val ACCOUNT_CONTEXT_INDICATORS = listOf(
        "a/c", "acct", "account", "card", "ending", "xxxx", "xx", "**",
        "vpa", "upi", "ref", "txn", "avl bal", "balance", "pos"
    )

    /**
     * Checks if a sender title matches a recognized bank or financial institution sender.
     */
    fun isBankSender(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        val cleanTitle = title.trim()

        // Match TRAI format like VK-HDFCBK, AD-ICICIB, etc.
        if (TRAI_BANK_HEADER_REGEX.matches(cleanTitle)) {
            return true
        }

        // Match known bank names
        val lower = cleanTitle.lowercase()
        return BANK_SENDER_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Determines whether a notification originating from an SMS / messaging app
     * is specifically a bank or financial transaction message (and not a personal chat).
     */
    fun isSpecificBankSmsNotification(
        title: String?,
        combinedText: String
    ): Boolean {
        if (combinedText.isBlank()) return false
        val lowerText = combinedText.lowercase()

        // 1. Must pass standard financial detection (keywords, amount, not OTP/spam)
        if (!isFinancialNotification(combinedText)) return false

        // 2. Must have account or transaction context
        val hasAccountContext = ACCOUNT_CONTEXT_INDICATORS.any { lowerText.contains(it) }

        // 3. Check sender title: either a known bank sender OR strong account context with explicit debit/credit
        val hasBankSender = isBankSender(title)

        if (hasBankSender && hasAccountContext) {
            return true
        }

        // If title is not standard TRAI header, require very strong bank signals in body:
        // (explicit debit/credit/negative amount + account reference + bank mention)
        val hasDebitOrCreditSignal = lowerText.contains("debited") || lowerText.contains("credited") ||
                lowerText.contains("spent") || lowerText.contains("paid") ||
                lowerText.contains("sent") || lowerText.contains("transferred") ||
                lowerText.contains("withdrawn") || lowerText.contains("purchase") ||
                lowerText.contains("charged") || lowerText.contains("dr.") || lowerText.contains("dr by") ||
                hasNegativeAmount(lowerText)

        val hasStrongBankBody = hasDebitOrCreditSignal &&
                hasAccountContext &&
                BANK_SENDER_KEYWORDS.any { lowerText.contains(it) }

        return hasStrongBankBody
    }

    /**
     * Determines whether a notification originating from an Email app (Gmail, Outlook, etc.)
     * is specifically a bank or financial transaction email (and not a personal or marketing email).
     */
    fun isSpecificBankEmailNotification(
        title: String?,
        combinedText: String
    ): Boolean {
        if (combinedText.isBlank()) return false
        val lowerText = combinedText.lowercase()

        // 1. Must pass standard financial detection (keywords, amount, not OTP/spam)
        if (!isFinancialNotification(combinedText)) return false

        // 2. Must have account or transaction context (A/c, card, ending, xx1234, etc.)
        val hasAccountContext = ACCOUNT_CONTEXT_INDICATORS.any { lowerText.contains(it) }

        // 3. Must have explicit transactional action
        val hasTransactionAction = lowerText.contains("debited") || lowerText.contains("credited") ||
                lowerText.contains("spent") || lowerText.contains("paid") ||
                lowerText.contains("withdrawn") || lowerText.contains("purchase") ||
                lowerText.contains("sent") || lowerText.contains("transferred") ||
                lowerText.contains("charged") || lowerText.contains("deducted") ||
                lowerText.contains("dr.") || lowerText.contains("dr by") ||
                hasNegativeAmount(lowerText)

        // 4. Must identify bank sender or bank keyword in body/title
        val hasBankSenderOrKeyword = isBankSender(title) ||
                BANK_SENDER_KEYWORDS.any { lowerText.contains(it) } ||
                Regex("""(?:alert|alerts|notification|statement|instaalert)""", RegexOption.IGNORE_CASE).containsMatchIn(title ?: "")

        return hasAccountContext && hasTransactionAction && hasBankSenderOrKeyword
    }
}
