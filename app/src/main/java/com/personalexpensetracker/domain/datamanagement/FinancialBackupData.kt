package com.personalexpensetracker.domain.datamanagement

import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurringExpense
import java.time.Instant

/**
 * Encapsulates the entire financial dataset for backup, export, import, and restore (Phase 9 Step 1).
 */
data class FinancialBackupData(
    val version: Int = 1,
    val exportedAt: Instant = Instant.now(),
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val recurringExpenses: List<RecurringExpense> = emptyList()
)

/**
 * Summary of results produced by an import or restore operation.
 */
data class ImportSummary(
    val expensesImported: Int = 0,
    val incomesImported: Int = 0,
    val budgetsImported: Int = 0,
    val recurringExpensesImported: Int = 0,
    val skippedRecords: Int = 0,
    val failedRecords: Int = 0,
    val errors: List<String> = emptyList()
) {
    val totalImported: Int
        get() = expensesImported + incomesImported + budgetsImported + recurringExpensesImported
}

/**
 * Generic result wrapper for export, import, backup, and restore operations.
 */
sealed class DataOperationResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : DataOperationResult<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : DataOperationResult<Nothing>()
}

