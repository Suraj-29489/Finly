package com.personalexpensetracker.ui.screens.dashboard

import com.personalexpensetracker.domain.model.Expense
import java.math.BigDecimal

/**
 * Represents the UI state for the Finly Dashboard screen.
 */
sealed interface DashboardUiState {

    /**
     * Initial loading state while observing expenses and balances.
     */
    data object Loading : DashboardUiState

    /**
     * Empty state when the database contains no recorded transactions.
     */
    data object Empty : DashboardUiState

    /**
     * Populated state with financial metrics, income/balance indicators, and recent transactions.
     *
     * @property currentMonthTotal Total spending during the current calendar month.
     * @property todayTotal Total spending recorded for today.
     * @property currentMonthTransactionCount Number of expense transactions in current calendar month.
     * @property recentTransactions The latest transactions recorded (up to 5), ordered newest-first.
     * @property netBalance Total financial balance (Total Income - Total Expenses).
     * @property totalIncome All-time total income recorded.
     * @property totalExpenses All-time total expenses recorded.
     * @property currentMonthIncome Total income recorded during the current calendar month.
     * @property currentMonthCashFlow Net cash flow for current month (Month Income - Month Expenses).
     * @property todayIncome Total income recorded today.
     * @property todayCashFlow Net cash flow recorded today (Today Income - Today Expenses).
     */
    data class Success(
        val currentMonthTotal: BigDecimal,
        val todayTotal: BigDecimal,
        val currentMonthTransactionCount: Int,
        val recentTransactions: List<Expense>,
        val netBalance: BigDecimal = BigDecimal.ZERO,
        val totalIncome: BigDecimal = BigDecimal.ZERO,
        val totalExpenses: BigDecimal = BigDecimal.ZERO,
        val currentMonthIncome: BigDecimal = BigDecimal.ZERO,
        val currentMonthCashFlow: BigDecimal = BigDecimal.ZERO,
        val todayIncome: BigDecimal = BigDecimal.ZERO,
        val todayCashFlow: BigDecimal = BigDecimal.ZERO,
        val topCategories: List<CategorySpend> = emptyList()
    ) : DashboardUiState
}

/**
 * Data class representing a category's spending total and share.
 */
data class CategorySpend(
    val category: String,
    val amount: BigDecimal,
    val percentage: Int = 0
)
