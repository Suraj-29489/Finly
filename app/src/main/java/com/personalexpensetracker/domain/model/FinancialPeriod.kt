package com.personalexpensetracker.domain.model

import java.math.BigDecimal
import java.time.YearMonth

/**
 * Supported periods for financial reporting and analytics (Phase 8 Step 6).
 */
enum class FinancialPeriod(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

/**
 * Monthly trend point holding cash flow data for a specific year and month.
 */
data class MonthlyCashFlowTrend(
    val yearMonth: YearMonth,
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val netCashFlow: BigDecimal = BigDecimal.ZERO
)

/**
 * Complete cash flow analytics report for a selected period.
 */
data class CashFlowAnalyticsReport(
    val period: FinancialPeriod,
    val summary: FinancialPeriodSummary,
    val monthlyTrends: List<MonthlyCashFlowTrend> = emptyList(),
    val incomeTransactionCount: Int = 0,
    val expenseTransactionCount: Int = 0
)

