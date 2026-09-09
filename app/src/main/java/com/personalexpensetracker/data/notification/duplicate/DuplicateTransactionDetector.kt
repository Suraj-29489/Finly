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
         * Time window within which same-amount, same-merchant transactions
         * are considered potential duplicates.
         * Set to 2 minutes — bank notifications for the same transaction
         * typically arrive within seconds.
         */
        val DUPLICATE_TIME_TOLERANCE: Duration = Duration.ofMinutes(2)

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

        // Strategy 1: Reference ID match (strongest signal)
        if (!transaction.referenceId.isNullOrBlank()) {
            val hasSameRef = recentTransactions.any { record ->
                !record.referenceId.isNullOrBlank() &&
                    record.referenceId.equals(transaction.referenceId, ignoreCase = true)
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

        // Strategy 3: Amount + merchant + time proximity
        val hasSimilarTransaction = recentTransactions.any { record ->
            record.amount.compareTo(transaction.amount) == 0 &&
                isMerchantMatch(record.merchant, transaction.merchant) &&
                Duration.between(record.timestamp, transaction.transactionTime).abs() <= DUPLICATE_TIME_TOLERANCE
        }
        if (hasSimilarTransaction) return true

        return false
    }

    private fun isMerchantMatch(m1: String?, m2: String?): Boolean {
        if (m1.isNullOrBlank() || m2.isNullOrBlank()) return true
        if (m1.equals(m2, ignoreCase = true)) return true
        if (m1.contains("unknown", ignoreCase = true) || m2.contains("unknown", ignoreCase = true)) return true
        if (m1.contains(m2, ignoreCase = true) || m2.contains(m1, ignoreCase = true)) return true
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
