package com.personalexpensetracker.data.notification

import com.personalexpensetracker.data.notification.category.MerchantCategoryResolver
import com.personalexpensetracker.data.notification.detection.FinancialNotificationDetector
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import com.personalexpensetracker.data.notification.duplicate.DuplicateTransactionDetector
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.notification.parser.TransactionParser
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import java.math.BigDecimal

/**
 * Full automatic expense capture pipeline processor.
 *
 * Chains together all pipeline stages:
 * 1. Financial notification detection
 * 2. Debit/Credit classification
 * 3. Transaction parsing (amount, merchant, ref ID)
 * 4. Category resolution
 * 5. Duplicate detection
 * 6. Expense creation through the existing repository
 *
 * ## Safety Rules
 * - Only DEBIT transactions create expenses.
 * - CREDIT and UNKNOWN transactions are ignored.
 * - Failed/pending/reversed transactions are ignored.
 * - Duplicate transactions are ignored.
 * - If amount cannot be parsed, no expense is created.
 * - Never crashes — all errors are captured.
 *
 * ## Architecture
 * - Uses the EXISTING [ExpenseRepository] for persistence.
 * - Uses the EXISTING [Category] system for categorization.
 * - Does NOT create a second database or category system.
 */
class AutoExpenseCaptureProcessor(
    private val expenseRepository: ExpenseRepository,
    private val duplicateDetector: DuplicateTransactionDetector = DuplicateTransactionDetector(),
    private val incomeRepository: IncomeRepository? = null
) : NotificationProcessor {

    override suspend fun process(notification: RawNotificationData): NotificationProcessingResult {
        return try {
            processInternal(notification)
        } catch (e: Exception) {
            NotificationProcessingResult.Error(
                message = "Pipeline error: ${e.message}",
                data = notification
            )
        }
    }

    private suspend fun processInternal(notification: RawNotificationData): NotificationProcessingResult {
        // Gate 0: Content check
        if (!notification.hasContent) {
            return NotificationProcessingResult.Ignored("No text content")
        }

        val combinedText = notification.combinedText

        // Gate 1: Financial notification detection
        if (!FinancialNotificationDetector.isFinancialNotification(combinedText)) {
            return NotificationProcessingResult.Ignored("Not a financial notification")
        }

        // Gate 2: Debit/Credit classification
        val direction = TransactionDirectionClassifier.classify(combinedText)
        when (direction) {
            TransactionDirection.CREDIT -> {
                if (incomeRepository == null) {
                    return NotificationProcessingResult.CreditIgnored(notification)
                }

                // If IncomeRepository is provided, parse and create Income record
                val parsedTransaction = TransactionParser.parse(notification, direction)
                    ?: return NotificationProcessingResult.InvalidTransaction(
                        reason = "Could not parse transaction amount",
                        data = notification
                    )

                if (parsedTransaction.amount <= BigDecimal.ZERO) {
                    return NotificationProcessingResult.InvalidTransaction(
                        reason = "Invalid amount: ${parsedTransaction.amount}",
                        data = notification
                    )
                }

                if (duplicateDetector.isDuplicate(parsedTransaction)) {
                    return NotificationProcessingResult.Duplicate(notification)
                }

                val source = if (parsedTransaction.sourcePackage == "sms") "SMS" else parsedTransaction.sourcePackage
                val income = Income(
                    title = parsedTransaction.merchant ?: "Income",
                    amount = parsedTransaction.amount,
                    source = source,
                    date = parsedTransaction.transactionTime,
                    notes = "Auto-captured from ${parsedTransaction.sourcePackage}"
                )

                val incomeId = incomeRepository.insertIncome(income)
                duplicateDetector.recordTransaction(parsedTransaction)

                return NotificationProcessingResult.IncomeCreated(
                    incomeId = incomeId,
                    parsedTransaction = parsedTransaction,
                    source = source
                )
            }
            TransactionDirection.UNKNOWN -> {
                return NotificationProcessingResult.Ambiguous(notification)
            }
            TransactionDirection.DEBIT -> {
                // Continue to parsing
            }
        }

        // Gate 3: Transaction parsing
        val parsedTransaction = TransactionParser.parse(notification, direction)
            ?: return NotificationProcessingResult.InvalidTransaction(
                reason = "Could not parse transaction amount",
                data = notification
            )

        // Validate amount
        if (parsedTransaction.amount <= BigDecimal.ZERO) {
            return NotificationProcessingResult.InvalidTransaction(
                reason = "Invalid amount: ${parsedTransaction.amount}",
                data = notification
            )
        }

        // Gate 4: Duplicate detection
        if (duplicateDetector.isDuplicate(parsedTransaction)) {
            return NotificationProcessingResult.Duplicate(notification)
        }

        // Gate 5: Category resolution
        val category = MerchantCategoryResolver.resolveCategory(parsedTransaction.merchant)

        // Gate 6: Create expense through existing data layer
        val expense = Expense(
            title = parsedTransaction.merchant ?: "Auto-captured expense",
            amount = parsedTransaction.amount,
            category = category,
            date = parsedTransaction.transactionTime,
            notes = "Auto-captured from ${parsedTransaction.sourcePackage}"
        )

        val expenseId = expenseRepository.insertExpense(expense)

        // Record for duplicate detection
        duplicateDetector.recordTransaction(parsedTransaction)

        return NotificationProcessingResult.ExpenseCreated(
            expenseId = expenseId,
            parsedTransaction = parsedTransaction,
            category = category
        )
    }
}
