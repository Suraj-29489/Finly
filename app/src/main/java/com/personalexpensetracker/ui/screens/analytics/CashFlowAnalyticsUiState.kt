package com.personalexpensetracker.ui.screens.analytics

import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.model.FinancialPeriodSummary
import com.personalexpensetracker.domain.model.MonthlyCashFlowTrend

/**
 * UI State for Cash Flow Analytics and Historical Reporting (Phase 8 Step 6).
 */
data class CashFlowAnalyticsUiState(
    val selectedPeriod: FinancialPeriod = FinancialPeriod.THIS_MONTH,
    val summary: FinancialPeriodSummary = FinancialPeriodSummary(),
    val monthlyTrends: List<MonthlyCashFlowTrend> = emptyList(),
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    val isLoading: Boolean = false
)

