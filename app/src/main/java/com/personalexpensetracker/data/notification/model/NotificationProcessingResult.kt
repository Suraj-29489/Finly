package com.personalexpensetracker.data.notification.model

import com.personalexpensetracker.data.notification.parser.ParsedTransaction

/**
 * Represents the processing outcome for a single notification received
 * by the automatic expense capture pipeline.
 *
 * Each notification passes through the pipeline and produces exactly one
 * [NotificationProcessingResult] that describes what happened to it.
 */
sealed class NotificationProcessingResult {

    /**
     * The notification was completely ignored — it contained no useful content
     * or was filtered out before any financial analysis.
     */
    data class Ignored(val reason: String) : NotificationProcessingResult()

    /**
     * The notification was received and passed into the pipeline for processing.
     * This is the initial result before financial detection runs.
     */
    data class Received(val data: RawNotificationData) : NotificationProcessingResult()

    /**
     * The notification was identified as a financial transaction but has not yet
     * been fully processed (future pipeline stages will refine this).
     */
    data class PendingProcessing(val data: RawNotificationData) : NotificationProcessingResult()

    /**
     * The notification was identified as a CREDIT (inflow) and was intentionally
     * NOT turned into an expense.
     */
    data class CreditIgnored(val data: RawNotificationData) : NotificationProcessingResult()

    /**
     * The notification was classified as ambiguous (direction could not be
     * confidently determined) and was NOT turned into an expense.
     */
    data class Ambiguous(val data: RawNotificationData) : NotificationProcessingResult()

    /**
     * The notification was detected as a failed/pending/reversed transaction
     * and was NOT turned into an expense.
     */
    data class InvalidTransaction(val reason: String, val data: RawNotificationData) : NotificationProcessingResult()

    /**
     * The notification was a duplicate of a previously processed transaction
     * and was NOT turned into an expense.
     */
    data class Duplicate(val data: RawNotificationData) : NotificationProcessingResult()

    /**
     * A DEBIT transaction was successfully parsed and an expense was created.
     */
    data class ExpenseCreated(
        val expenseId: Long,
        val parsedTransaction: ParsedTransaction,
        val category: String
    ) : NotificationProcessingResult()

    /**
     * A CREDIT transaction was successfully parsed and an income record was created.
     */
    data class IncomeCreated(
        val incomeId: Long,
        val parsedTransaction: ParsedTransaction,
        val source: String
    ) : NotificationProcessingResult()

    /**
     * An error occurred during processing. The notification was not turned into
     * an expense. The service did NOT crash.
     */
    data class Error(val message: String, val data: RawNotificationData? = null) : NotificationProcessingResult()
}
