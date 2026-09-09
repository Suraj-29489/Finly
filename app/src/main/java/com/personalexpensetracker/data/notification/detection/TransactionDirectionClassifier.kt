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
     */
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit",
        "amount deducted", "amount paid",
        "payment successful", "payment made", "payment of", "payment to", "payment for",
        "paid to", "paid for", "paid at", "paid",
        "spent on", "spent at", "spent",
        "purchase at", "purchase from", "purchased",
        "withdrawn", "withdrawal",
        "transferred to", "sent to", "money sent",
        "upi payment", "upi debit", "using upi", "via upi",
        "card payment", "card purchase", "card transaction", "via card",
        "charged on", "charged to", "charged by", "charged",
        "pos transaction",
        "auto-debit", "auto debit", "emi deducted",
        "bill payment", "bill paid",
        "mandate executed"
    )

    /**
     * Keywords that indicate money ENTERING the account (credit/inflow).
     * These MUST NOT be treated as expenses.
     */
    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit",
        "amount credited", "amount received",
        "received from", "money received",
        "salary credited", "salary received",
        "refund received", "refund credited", "refund processed",
        "cashback credited", "cashback received",
        "deposit", "deposited", "cash deposited",
        "transferred from", "received to",
        "upi credit",
        "interest credited",
        "dividend credited",
        "bonus credited"
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
     * Classifies a notification's financial direction.
     *
     * @param combinedText All available text from the notification.
     * @return The classified [TransactionDirection].
     */
    fun classify(combinedText: String): TransactionDirection {
        if (combinedText.isBlank()) return TransactionDirection.UNKNOWN

        val lowerText = combinedText.lowercase()

        // Check for failed/reversed transactions first — these are not actionable
        if (FAILED_KEYWORDS.any { lowerText.contains(it) }) {
            // If the transaction failed, it's effectively UNKNOWN (don't create expense)
            return TransactionDirection.UNKNOWN
        }

        // Mask "credit card" so the payment instrument name doesn't falsely trigger CREDIT direction
        val textForCreditCheck = lowerText
            .replace("credit card", "card")
            .replace("credit a/c", "acct")

        val debitScore = calculateScore(lowerText, DEBIT_KEYWORDS) + calculateStructuralDebitScore(lowerText)
        val creditScore = calculateScore(textForCreditCheck, CREDIT_KEYWORDS)

        return when {
            // Clear debit signal with no credit conflict
            debitScore > 0 && creditScore == 0 -> TransactionDirection.DEBIT
            // Clear credit signal with no debit conflict
            creditScore > 0 && debitScore == 0 -> TransactionDirection.CREDIT
            // Both signals present — ambiguous or weighted
            debitScore > 0 && creditScore > 0 -> {
                // If debit signal is significantly stronger, cautiously classify as DEBIT
                if (debitScore >= creditScore * 2) TransactionDirection.DEBIT
                // If credit signal is significantly stronger, classify as CREDIT
                else if (creditScore >= debitScore * 2) TransactionDirection.CREDIT
                // Otherwise, ambiguous — safety first
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
        // Check for outgoing transfer with intervening text/amount: "sent ... to" or "transferred ... to"
        val outgoingTransfer = Regex("""\b(?:sent|transferred)\b.+\bto\b""", RegexOption.IGNORE_CASE)
        if (outgoingTransfer.containsMatchIn(lowerText) &&
            !lowerText.contains("from") && !lowerText.contains("received")
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
