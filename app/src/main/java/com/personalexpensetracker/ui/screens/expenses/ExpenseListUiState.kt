package com.personalexpensetracker.ui.screens.expenses

import com.personalexpensetracker.domain.model.Expense

/**
 * UI State representing the Expense List screen.
 */
sealed interface ExpenseListUiState {
    data object Loading : ExpenseListUiState
    data object Empty : ExpenseListUiState
    data object NoMatchingResults : ExpenseListUiState
    data class Success(val expenses: List<Expense>) : ExpenseListUiState
    data class Error(val message: String) : ExpenseListUiState
}

/**
 * Sorting options for expenses list.
 */
enum class ExpenseSortOrder(val displayName: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
    AMOUNT_HIGH_TO_LOW("Amount: High to Low"),
    AMOUNT_LOW_TO_HIGH("Amount: Low to High")
}

/**
 * Date filtering options for expenses list.
 */
enum class ExpenseDateFilter(val displayName: String) {
    ALL("All Dates"),
    TODAY("Today"),
    THIS_MONTH("This Month")
}

