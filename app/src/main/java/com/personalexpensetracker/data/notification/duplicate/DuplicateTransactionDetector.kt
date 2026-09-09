package com.personalexpensetracker.data.notification.duplicate

import com.personalexpensetracker.data.notification.parser.ParsedTransaction
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Protects against duplicate transaction creation from repeated or updated
 * notifications.
 *
 * ## Strategy
 * 1. If a reference ID is available, use it for exact deduplication.
 * 2. Otherwise, use a combination of amount + merchant + direction + time
 *    with a reasonable time tolerance.
 *
 * ## Balance
 * - NOT so aggressive that two legitimate separate purchases are
 *   incorrectly treated as one.
 * - NOT so loose that the same notification creates multiple expenses.
 *
 * ## Thread Safety
 * Uses [ConcurrentLinkedDeque] for thread-safe access from the
 * NotificationListenerService's coroutine scope.
 */
class DuplicateTransactionDetector {

    companion object {
        /**
         * Time window within which same-amount, same-merchant or same-account
         * transactions are considered potential duplicates across channels (SMS vs Email).
         * Set to 60 minutes to account for email background sync delays.
         */
        val DUPLICATE_TIME_TOLERANCE: Duration = Duration.ofMinutes(60)

        /**
         * Maximum number of recent transactions to keep in memory.
         * Older entries are evicted to prevent unbounded memory growth.
         */
        const val MAX_RECENT_ENTRIES = 200

        /**
         * Time after which entries are eligible for eviction regardless.
         */
        val ENTRY_EXPIRY: Duration = Duration.ofHours(24)
    }

    /**
     * Internal record of a recently processed transaction for dedup purposes.
     */
    private data class TransactionRecord(
        val amount: BigDecimal,
        val merchant: String?,
        val referenceId: String?,
        val notificationKey: String?,
        val accountLast4: String?,
        val timestamp: Instant
    )

    private val recentTransactions = ConcurrentLinkedDeque<TransactionRecord>()

    /**
     * Checks whether the given transaction is a duplicate of a recently
     * processed one.
     *
     * @param transaction The parsed transaction to check.
     * @return true if this transaction is a duplicate and should NOT create an expense.
     */
    fun isDuplicate(transaction: ParsedTransaction): Boolean {
        evictExpiredEntries()

        // Strategy 1: Reference ID match (strongest signal, accounts for prefix formatting)
        if (!transaction.referenceId.isNullOrBlank()) {
            val hasSameRef = recentTransactions.any { record ->
                isReferenceMatch(record.referenceId, transaction.referenceId)
            }
            if (hasSameRef) return true
        }

        // Strategy 2: Notification key match (same Android notification updated)
        if (!transaction.notificationKey.isNullOrBlank()) {
            val hasSameKey = recentTransactions.any { record ->
                !record.notificationKey.isNullOrBlank() &&
                    record.notificationKey == transaction.notificationKey
            }
            if (hasSameKey) return true
        }

        // Strategy 3: Same amount + same account last-4 digits within time tolerance (Cross-channel SMS vs Email)
        if (!transaction.accountLast4.isNullOrBlank()) {
            val hasSameAccountAndAmount = recentTransactions.any { record ->
                record.amount.compareTo(transaction.amount) == 0 &&
                    !record.accountLast4.isNullOrBlank() &&
                    record.accountLast4 == transaction.accountLast4 &&
                    Duration.between(record.timestamp, transaction.transactionTime).abs() <= DUPLICATE_TIME_TOLERANCE
            }
            if (hasSameAccountAndAmount) return true
        }

        // Strategy 4: Amount + merchant + time proximity
        val hasSimilarTransaction = recentTransactions.any { record ->
            record.amount.compareTo(transaction.amount) == 0 &&
                isMerchantMatch(record.merchant, transaction.merchant) &&
                Duration.between(record.timestamp, transaction.transactionTime).abs() <= DUPLICATE_TIME_TOLERANCE
        }
        if (hasSimilarTransaction) return true

        return false
    }

    internal fun isReferenceMatch(ref1: String?, ref2: String?): Boolean {
        if (ref1.isNullOrBlank() || ref2.isNullOrBlank()) return false
        val clean1 = ref1.trim()
        val clean2 = ref2.trim()
        if (clean1.equals(clean2, ignoreCase = true)) return true

        // Check if one contains the other (e.g. "UPI778899" vs "778899")
        if (clean1.length >= 6 && clean2.length >= 6) {
            if (clean1.contains(clean2, ignoreCase = true) || clean2.contains(clean1, ignoreCase = true)) {
                return true
            }
            val digits1 = clean1.filter { it.isDigit() }
            val digits2 = clean2.filter { it.isDigit() }
            if (digits1.length >= 6 && digits2.length >= 6) {
                if (digits1 == digits2 || digits1.contains(digits2) || digits2.contains(digits1)) {
                    return true
                }
            }
        }
        return false
    }

    internal fun isMerchantMatch(m1: String?, m2: String?): Boolean {
        if (m1.isNullOrBlank() || m2.isNullOrBlank()) return true
        val clean1 = m1.trim().lowercase()
        val clean2 = m2.trim().lowercase()
        if (clean1 == clean2) return true
        if (clean1.contains("unknown") || clean2.contains("unknown")) return true
        if (clean1.contains("auto-captured") || clean2.contains("auto-captured")) return true
        if (clean1.contains("income") || clean2.contains("income")) return true

        // If one is a generic bank or alert title, treat as compatible
        val bankKeywords = listOf("bank", "hdfc", "sbi", "icici", "axis", "kotak", "pnb", "alert", "instaalert", "messaging")
        if (bankKeywords.any { clean1.contains(it) } || bankKeywords.any { clean2.contains(it) }) {
            return true
        }

        if (clean1.contains(clean2) || clean2.contains(clean1)) return true
        return false
    }

    /**
     * Records a transaction that was successfully processed.
     * Call this AFTER the expense has been created to mark it as "seen".
     *
     * @param transaction The parsed transaction to record.
     */
    fun recordTransaction(transaction: ParsedTransaction) {
        val record = TransactionRecord(
            amount = transaction.amount,
            merchant = transaction.merchant,
            referenceId = transaction.referenceId,
            notificationKey = transaction.notificationKey,
            accountLast4 = transaction.accountLast4,
            timestamp = transaction.transactionTime
        )
        recentTransactions.addFirst(record)

        // Cap size
        while (recentTransactions.size > MAX_RECENT_ENTRIES) {
            recentTransactions.removeLast()
        }
    }

    /**
     * Removes entries older than [ENTRY_EXPIRY].
     */
    private fun evictExpiredEntries() {
        val cutoff = Instant.now().minus(ENTRY_EXPIRY)
        recentTransactions.removeAll { it.timestamp.isBefore(cutoff) }
    }

    /**
     * Clears all recorded transactions. Useful for testing.
     */
    fun clear() {
        recentTransactions.clear()
    }

    /**
     * Returns the number of currently tracked transactions.
     */
    fun size(): Int = recentTransactions.size
}
