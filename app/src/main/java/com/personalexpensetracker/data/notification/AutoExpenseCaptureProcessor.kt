package com.personalexpensetracker.data.notification

import com.personalexpensetracker.data.notification.category.MerchantCategoryResolver
import com.personalexpensetracker.data.notification.detection.FinancialNotificationDetector
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import com.personalexpensetracker.data.notification.duplicate.DuplicateTransactionDetector
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.notification.parser.ParsedTransaction
import com.personalexpensetracker.data.notification.parser.SourceAwareParser
import com.personalexpensetracker.data.notification.parser.TransactionParser
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal

/**
 * Full automatic expense capture pipeline processor.
 *
 * Chains together all pipeline stages:
 * 1. Financial notification detection
 * 2. Debit/Credit classification
 * 3. Transaction parsing (amount, merchant, ref ID)
 * 4. Category resolution
 * 5. Duplicate detection (In-memory & Persistent Room DB)
 * 6. Expense creation through the existing repository
 *
 * ## Safety Rules
 * - Only DEBIT transactions create expenses.
 * - CREDIT and UNKNOWN transactions are ignored/skipped.
 * - Failed/pending/reversed transactions are ignored.
 * - Duplicate transactions (cross-channel SMS vs Email) are ignored.
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

        // Gate 1: Financial notification detection & Bank SMS / Email filtering
        val isMessagingApp = SourceAwareParser.isMessagingApp(notification.packageName)
        val isEmailApp = SourceAwareParser.isEmailApp(notification.packageName)

        if (isMessagingApp) {
            // If the notification came from an SMS/default messaging app, verify it is specifically
            // a bank transaction message (reject personal chats, contact messages, and non-bank spam)
            if (!FinancialNotificationDetector.isSpecificBankSmsNotification(notification.title, combinedText)) {
                return NotificationProcessingResult.Ignored("Not a specific bank transaction message")
            }
        } else if (isEmailApp) {
            // If the notification came from an Email app (Gmail, Outlook, etc.), verify it is specifically
            // a bank/financial transaction email (reject personal or marketing emails)
            if (!FinancialNotificationDetector.isSpecificBankEmailNotification(notification.title, combinedText)) {
                return NotificationProcessingResult.Ignored("Not a specific bank transaction email")
            }
        } else {
            // For native banking and UPI apps, perform standard financial detection
            if (!FinancialNotificationDetector.isFinancialNotification(combinedText)) {
                return NotificationProcessingResult.Ignored("Not a financial notification")
            }
        }

        // Gate 2: Debit/Credit classification
        val direction = TransactionDirectionClassifier.classify(combinedText)
        when (direction) {
            TransactionDirection.CREDIT -> {
                // User requirement: "i want it read just the debit messages and skip credit messages"
                if (incomeRepository == null) {
                    return NotificationProcessingResult.CreditIgnored(notification)
                }

                // If IncomeRepository is explicitly provided, parse and create Income record
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

                val source = if (parsedTransaction.sourcePackage == "sms") "SMS"
                else if (isMessagingApp) "Bank SMS"
                else if (isEmailApp) "Bank Email"
                else parsedTransaction.sourcePackage

                val income = Income(
                    title = parsedTransaction.merchant ?: "Income",
                    amount = parsedTransaction.amount,
                    source = source,
                    date = parsedTransaction.transactionTime,
                    notes = "Auto-captured from $source"
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

        // Gate 4: Duplicate detection (both in-memory and persistent Room DB)
        val tolerance = DuplicateTransactionDetector.DUPLICATE_TIME_TOLERANCE
        val windowStart = parsedTransaction.transactionTime.minus(tolerance)
        val windowEnd = parsedTransaction.transactionTime.plus(tolerance)
        val existingExpenses = try {
            expenseRepository.getExpensesByDateRange(windowStart, windowEnd).firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val existingDuplicate = existingExpenses.firstOrNull { existing ->
            existing.amount.compareTo(parsedTransaction.amount) == 0 &&
                (isPersistentExpenseMatch(existing, parsedTransaction) || duplicateDetector.isDuplicate(parsedTransaction))
        }

        val isDuplicate = existingDuplicate != null || duplicateDetector.isDuplicate(parsedTransaction)

        if (isDuplicate) {
            // Enrich existing expense if it had a generic/unknown title and new notification has a specific merchant
            if (existingDuplicate != null && isGenericTitle(existingDuplicate.title) && !isGenericTitle(parsedTransaction.merchant)) {
                val newCategory = MerchantCategoryResolver.resolveCategory(parsedTransaction.merchant)
                val updatedExpense = existingDuplicate.copy(
                    title = parsedTransaction.merchant ?: existingDuplicate.title,
                    category = newCategory
                )
                expenseRepository.updateExpense(updatedExpense)
            }
            duplicateDetector.recordTransaction(parsedTransaction)
            return NotificationProcessingResult.Duplicate(notification)
        }

        // Gate 5: Category resolution
        val category = MerchantCategoryResolver.resolveCategory(parsedTransaction.merchant)

        // Gate 6: Create expense through existing data layer
        val sourceLabel = if (isMessagingApp) {
            if (FinancialNotificationDetector.isBankSender(notification.title)) {
                notification.title?.trim() ?: "Bank SMS"
            } else "Bank SMS"
        } else if (isEmailApp) {
            if (FinancialNotificationDetector.isBankSender(notification.title)) {
                notification.title?.trim() ?: "Bank Email"
            } else "Bank Email"
        } else parsedTransaction.sourcePackage

        val metadataNotes = buildString {
            append("Auto-captured from $sourceLabel")
            if (!parsedTransaction.referenceId.isNullOrBlank()) {
                append(" [ref:${parsedTransaction.referenceId}]")
            }
            if (!parsedTransaction.accountLast4.isNullOrBlank()) {
                append(" [acct:${parsedTransaction.accountLast4}]")
            }
        }

        val expense = Expense(
            title = parsedTransaction.merchant ?: "Auto-captured expense",
            amount = parsedTransaction.amount,
            category = category,
            date = parsedTransaction.transactionTime,
            notes = metadataNotes
        )

        val expenseId = expenseRepository.insertExpense(expense)

        // Record for in-memory duplicate detection
        duplicateDetector.recordTransaction(parsedTransaction)

        return NotificationProcessingResult.ExpenseCreated(
            expenseId = expenseId,
            parsedTransaction = parsedTransaction,
            category = category
        )
    }

    private fun isPersistentExpenseMatch(existing: Expense, parsed: ParsedTransaction): Boolean {
        val notes = existing.notes.orEmpty()
        // 1. Check matching reference ID if available
        if (!parsed.referenceId.isNullOrBlank() && notes.contains("[ref:${parsed.referenceId}]")) {
            return true
        }
        // 2. Check matching accountLast4 if available
        if (!parsed.accountLast4.isNullOrBlank() && notes.contains("[acct:${parsed.accountLast4}]")) {
            return true
        }
        // 3. Check compatible merchant
        if (duplicateDetector.isMerchantMatch(existing.title, parsed.merchant)) {
            return true
        }
        return false
    }

    private fun isGenericTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return true
        val lower = title.trim().lowercase()
        return lower == "auto-captured expense" ||
               lower == "unknown merchant" ||
               lower.startsWith("bank sms") ||
               lower.startsWith("bank email") ||
               listOf("bank", "hdfc", "sbi", "icici", "axis", "kotak", "pnb", "alert", "instaalert").any { lower.contains(it) }
    }
}
