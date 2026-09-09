package com.personalexpensetracker.domain.model

import java.math.BigDecimal

/**
 * Encapsulates financial income, expenses, and net cash flow for a specific period.
 */
data class FinancialPeriodSummary(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val netCashFlow: BigDecimal = BigDecimal.ZERO
)

/**
 * Complete central financial balance model containing all-time metrics,
 * current month summary, and today summary (Phase 8 Step 4).
 */
data class FinancialBalance(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val currentMonth: FinancialPeriodSummary = FinancialPeriodSummary(),
    val today: FinancialPeriodSummary = FinancialPeriodSummary()
)

