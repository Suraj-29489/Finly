package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.FinancialPeriodSummary
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Instant

/**
 * Use case to observe cash flow for a designated date range [startDate, endDate] (Phase 8 Step 4).
 */
class GetPeriodCashFlowUseCase(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {

    operator fun invoke(startDate: Instant, endDate: Instant): Flow<FinancialPeriodSummary> {
        return combine(
            incomeRepository.getIncomesByDateRange(startDate, endDate),
            expenseRepository.getExpensesByDateRange(startDate, endDate)
        ) { incomes, expenses ->
            val totalIncome = incomes.fold(BigDecimal.ZERO) { acc, inc -> acc + inc.amount }
            val totalExpenses = expenses.fold(BigDecimal.ZERO) { acc, exp -> acc + exp.amount }
            val netCashFlow = totalIncome.subtract(totalExpenses)

            FinancialPeriodSummary(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netCashFlow = netCashFlow
            )
        }
    }
}

