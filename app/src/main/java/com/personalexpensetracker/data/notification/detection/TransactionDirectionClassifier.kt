package com.personalexpensetracker.data.notification.detection

/**
 * Classifies the direction of a financial transaction as DEBIT, CREDIT, or UNKNOWN.
 *
 * This is a critical safety gate: only DEBIT transactions may enter the automatic
 * expense creation pipeline. CREDIT and UNKNOWN transactions are never auto-created
 * as expenses.
 */
object TransactionDirectionClassifier {

    /**
     * The classified direction of a financial transaction.
     */
    enum class TransactionDirection {
        /** Money leaving the user's account — eligible for auto-expense creation. */
        DEBIT,
        /** Money entering the user's account — MUST NOT become an expense. */
        CREDIT,
        /** Direction cannot be confidently determined — MUST NOT become an expense. */
        UNKNOWN
    }

    /**
     * Keywords that indicate money LEAVING the account (debit/outflow).
     * Ordered from most specific to least specific.
     * Note: Payment channels/rails like "via upi", "using upi", "via card" describe
     * instruments, not directions, and are intentionally omitted.
     */
    /**
     * Keywords that indicate money LEAVING the account (debit/outflow).
     * Ordered from most specific to least specific.
     * Note: Payment channels/rails like "via upi", "using upi", "via card" describe
     * instruments, not directions, and are intentionally omitted.
     */
    private val DEBIT_KEYWORDS = listOf(
        "debited from", "debited to", "debited with", "debited by", "debited for",
        "has been debited", "is debited", "was debited", "debited", "debit",
        "amount debited", "amount deducted", "deducted from", "deducted for", "deducted by", "deducted",
        "amount paid", "payment successful", "payment made", "payment of", "payment to", "payment for",
        "paid to", "paid for", "paid at", "paid",
        "spent on", "spent at", "spent",
        "purchase at", "purchase from", "purchase of", "purchased at", "purchased", "purchase",
        "withdrawn from", "withdrawn at", "withdrawn of", "withdrawn",
        "withdrawal of", "withdrawal", "cash withdrawn", "cash withdrawal", "atm withdrawal", "atm wdl", "atm w/d",
        "transferred to", "transferred for", "transfer to",
        "sent to", "money sent to", "money sent",
        "upi debit", "card purchase",
        "charged on", "charged to", "charged by", "charged",
        "pos transaction", "pos txn",
        "auto-debit", "auto debit", "emi deducted", "nach debit",
        "bill payment", "bill paid",
        "mandate executed", "standing instruction",
        "dr for", "dr by", "dr with", "is dr", "dr.", " dr "
    )

    /**
     * Keywords that indicate money ENTERING the account (credit/inflow).
     * These MUST NOT be treated as expenses.
     */
    private val CREDIT_KEYWORDS = listOf(
        "credited to", "credited with", "credited by", "credited on", "credited for", "credited towards",
        "has been credited", "is credited", "was credited", "credited", "credit",
        "amount credited", "amount received",
        "payment received", "payment from",
        "received from", "received in your account", "received in your a/c", "received into", "received to", "received",
        "money received", "salary credited", "salary received",
        "refund received", "refund credited", "refund processed",
        "cashback credited", "cashback received",
        "deposit", "deposited", "cash deposited", "cheque deposited",
        "transferred from", "transferred to your account", "transferred to your a/c",
        "upi credit", "money added", "funds added",
        "inward credit", "inward rem", "inward remittance",
        "interest credited", "dividend credited", "bonus credited"
    )

    /**
     * Explicit keywords that definitively signal money entering the user's account.
     */
    private val EXPLICIT_CREDIT_KEYWORDS = listOf(
        "credited to", "credited with", "credited by", "credited on", "credited for", "credited towards",
        "has been credited", "is credited", "was credited", "credited",
        "amount credited", "amount received",
        "payment received", "payment from",
        "received in your account", "received in your a/c", "received from", "money received",
        "salary credited", "refund credited", "cashback credited",
        "transferred to your account", "transferred to your a/c", "transferred from",
        "deposited", "inward credit", "inward rem", "inward remittance",
        "interest credited", "dividend credited", "bonus credited"
    )

    /**
     * Explicit keywords that definitively signal money leaving the user's account.
     */
    private val EXPLICIT_DEBIT_KEYWORDS = listOf(
        "debited from", "debited with", "debited by", "debited for", "debited to", "has been debited",
        "is debited", "was debited", "debited", "amount debited", "amount deducted",
        "deducted from", "deducted for", "deducted by", "deducted",
        "spent on", "spent at", "spent",
        "paid to", "paid for", "paid at", "paid",
        "withdrawn from", "withdrawn at", "cash withdrawn", "cash withdrawal",
        "withdrawn", "withdrawal", "atm withdrawal",
        "purchase at", "purchase from", "purchase of", "purchased at", "purchased",
        "auto-debit", "auto debit", "emi deducted", "nach debit",
        "bill payment", "bill paid",
        "payment successful", "payment made",
        "charged on", "charged to", "charged by", "charged",
        "sent to", "money sent to", "money sent",
        "transferred to",
        "dr for", "dr by", "dr with", "dr. for", "dr. by"
    )

    /**
     * Matches negative amounts representing deduction or expense outflow:
     * - ₹500, -500, -1, -1.00, -Rs 100, -Rs. 50, -INR 250, -$10, ₹-500, Rs.-100
     */
    private val NEGATIVE_AMOUNT_REGEX = Regex(
        """(?:^|[\s:;(\[|])(?:[-−–—]\s*(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*)?|(?:₹|[Rr][Ss]\.?\s*|INR\s*|\$\s*)\s*[-−–—]\s*)(\d{1,3}(?:,\d{2,3})*|\d+)(?:\.\d{1,2})?(?!\d)"""
    )

    /**
     * Context prefixes that indicate an amount is an account balance or limit, NOT an expense.
     */
    private val BALANCE_PREFIX_REGEX = Regex(
        """(?:avl\s*bal|available\s*bal(?:ance)?|tot\s*bal|total\s*bal(?:ance)?|closing\s*bal(?:ance)?|clear\s*bal(?:ance)?|ledger\s*bal(?:ance)?|ac\s*bal(?:ance)?|rem\s*bal(?:ance)?|bal(?:ance)?[:\s]|limit[:\s]|credit\s*limit[:\s]|avl\s*lmt[:\s])""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Keywords indicating failed or reversed transactions — should NOT create expenses.
     */
    private val FAILED_KEYWORDS = listOf(
        "failed", "failure", "declined", "rejected",
        "reversed", "reversal", "cancelled", "canceled",
        "unsuccessful", "not successful", "could not",
        "pending", "on hold", "processing"
    )

    /**
     * Checks if the text contains a negative amount that is NOT part of a balance or limit statement.
     * Negative terms like -1, -500, -₹500 represent account deductions/debits.
     */
    private fun hasNegativeDebitAmount(text: String): Boolean {
        for (match in NEGATIVE_AMOUNT_REGEX.findAll(text)) {
            val start = match.range.first
            val preceding = text.substring((start - 30).coerceAtLeast(0), start)
            if (!BALANCE_PREFIX_REGEX.containsMatchIn(preceding)) {
                return true
            }
        }
        return false
    }

    /**
     * Classifies a notification's financial direction.
     *
     * @param combinedText All available text from the notification.
     * @return The classified [TransactionDirection].
     */
    fun classify(combinedText: String): TransactionDirection {
        if (combinedText.isBlank()) return TransactionDirection.UNKNOWN

        val lowerText = combinedText.lowercase()

        // Check for failed/reversed transactions first — these are not actionable
        // Note: If text explicitly mentions a refund received/credited, "cancelled" refers to
        // the underlying merchant order that was refunded, not a failed financial transaction.
        val isRefund = lowerText.contains("refund")
        if (!isRefund && FAILED_KEYWORDS.any { lowerText.contains(it) }) {
            return TransactionDirection.UNKNOWN
        }

        // Mask non-directional uses of "credit" so card/instrument names don't falsely trigger CREDIT
        val textForCreditCheck = lowerText
            .replace("credit card", "card")
            .replace("credit limit", "limit")
            .replace("credit line", "line")
            .replace("credit facility", "facility")
            .replace("line of credit", "line")
            .replace("credit a/c", "acct")
            .replace("credit acct", "acct")
            .replace("credit account", "acct")

        // Mask non-directional uses of "debit" (e.g. "debit card" instrument) and inward transfers
        val textForDebitCheck = lowerText
            .replace("debit card", "card")
            .replace("to credit card", "to card")
            .replace("transferred to your account", "transferred_inward")
            .replace("transferred to your a/c", "transferred_inward")
            .replace("transferred to your acct", "transferred_inward")
            .replace("sent to your account", "sent_inward")
            .replace("sent to your a/c", "sent_inward")
            .replace("sent to your acct", "sent_inward")

        // Regex patterns for actions where amount or vendor intervenes between keywords
        val receivedInAccountRegex = Regex("""\breceived\b.+\b(?:in|into|to)\s+(?:your\s+)?(?:a/?c|acct|account|savings)\b""", RegexOption.IGNORE_CASE)
        val receivedFromRegex = Regex("""\breceived\b.+\bfrom\b""", RegexOption.IGNORE_CASE)

        val hasExplicitCredit = EXPLICIT_CREDIT_KEYWORDS.any { textForCreditCheck.contains(it) } ||
                receivedInAccountRegex.containsMatchIn(textForCreditCheck) ||
                receivedFromRegex.containsMatchIn(textForCreditCheck)

        val paymentSuccessfulRegex = Regex("""\bpayment\b.+\b(?:was\s+)?successful\b""", RegexOption.IGNORE_CASE)
        val paymentToRegex = Regex("""\bpayment\b.+\bto\b(?!\s+(?:your\s+)?(?:a/?c|acct|account))""", RegexOption.IGNORE_CASE)
        val drRegex = Regex("""\b(?:is\s+)?dr\.?\s+(?:for|by|with|to)\b|\bdr\.?\s*[-−–—]?(?:₹|[Rr][Ss]\.?|INR|\$)\b""", RegexOption.IGNORE_CASE)
        val sentAmountRegex = Regex("""\b(?:you\s+)?sent\s+[-−–—]?(?:₹|[Rr][Ss]\.?|INR|\$)?\s*\d+""", RegexOption.IGNORE_CASE)
        val hasNegativeAmount = hasNegativeDebitAmount(textForDebitCheck)

        val hasExplicitDebit = EXPLICIT_DEBIT_KEYWORDS.any { textForDebitCheck.contains(it) } ||
                paymentSuccessfulRegex.containsMatchIn(textForDebitCheck) ||
                paymentToRegex.containsMatchIn(textForDebitCheck) ||
                drRegex.containsMatchIn(textForDebitCheck) ||
                sentAmountRegex.containsMatchIn(textForDebitCheck) ||
                hasNegativeAmount

        // Rule 1: Definitive explicit signal with no conflicting explicit action
        if (hasExplicitCredit && !hasExplicitDebit) {
            return TransactionDirection.CREDIT
        }
        if (hasExplicitDebit && !hasExplicitCredit) {
            return TransactionDirection.DEBIT
        }

        val debitScore = calculateScore(textForDebitCheck, DEBIT_KEYWORDS) + calculateStructuralDebitScore(textForDebitCheck)
        val creditScore = calculateScore(textForCreditCheck, CREDIT_KEYWORDS)

        return when {
            // Clear debit signal with no credit conflict
            debitScore > 0 && creditScore == 0 -> TransactionDirection.DEBIT
            // Clear credit signal with no debit conflict
            creditScore > 0 && debitScore == 0 -> TransactionDirection.CREDIT
            // Both signals present — weighted comparison
            debitScore > 0 && creditScore > 0 -> {
                if (debitScore >= creditScore * 2) TransactionDirection.DEBIT
                else if (creditScore >= debitScore * 2) TransactionDirection.CREDIT
                else TransactionDirection.UNKNOWN
            }
            // No signal at all
            else -> TransactionDirection.UNKNOWN
        }
    }

    /**
     * Additional score for structural debit patterns like "Sent ₹500 to Ramesh"
     * or "Transferred INR 1,000 to Swiggy" where amount separates the verb and recipient.
     */
    private fun calculateStructuralDebitScore(lowerText: String): Int {
        var score = 0
        // Check for outgoing transfer: "sent ... to" or "transferred ... to"
        // Explicitly exclude "transferred ... to your a/c" or "transferred ... to your account"
        val outgoingTransfer = Regex("""\b(?:sent|transferred)\b.+\bto\b(?!\s+(?:your\s+)?(?:a/?c|acct|account))""", RegexOption.IGNORE_CASE)
        if (outgoingTransfer.containsMatchIn(lowerText) &&
            !lowerText.contains("from") && !lowerText.contains("received") && !lowerText.contains("credited")
        ) {
            score += 15
        }

        // Check for "purchase of" / "order placed" / "payment successful"
        if (lowerText.contains("purchase of") || lowerText.contains("order placed") ||
            lowerText.contains("payment successful") || lowerText.contains("debited by") ||
            lowerText.contains("amount debited") || lowerText.contains("debited for") ||
            lowerText.contains("deducted for") || lowerText.contains("deducted by")
        ) {
            score += 10
        }

        // Negative amounts strongly indicate debit
        if (hasNegativeDebitAmount(lowerText)) {
            score += 20
        }

        return score
    }

    /**
     * Calculates a keyword match score for a set of keywords.
     * More specific (longer) keyword matches score higher.
     */
    private fun calculateScore(text: String, keywords: List<String>): Int {
        return keywords.sumOf { keyword ->
            if (text.contains(keyword)) keyword.length else 0
        }
    }
}
