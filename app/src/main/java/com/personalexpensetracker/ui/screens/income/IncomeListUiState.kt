package com.personalexpensetracker.ui.screens.income

import com.personalexpensetracker.domain.model.Income
import java.math.BigDecimal

/**
 * Filter options for income date.
 */
enum class IncomeDateFilter(val displayName: String) {
    ALL("All Dates"),
    TODAY("Today"),
    THIS_MONTH("This Month")
}

/**
 * Sort orders for the income list.
 */
enum class IncomeSortOrder(val displayName: String) {
    NEWEST_FIRST("Sort: Newest first"),
    OLDEST_FIRST("Sort: Oldest first"),
    HIGHEST_AMOUNT("Sort: Highest amount"),
    LOWEST_AMOUNT("Sort: Lowest amount")
}

/**
 * UI State for the Income List screen.
 */
sealed interface IncomeListUiState {
    object Loading : IncomeListUiState

    data class Success(
        val incomes: List<Income>,
        val totalInflow: BigDecimal
    ) : IncomeListUiState

    data class Empty(
        val isFiltered: Boolean = false
    ) : IncomeListUiState

    data class Error(
        val message: String
    ) : IncomeListUiState
}

