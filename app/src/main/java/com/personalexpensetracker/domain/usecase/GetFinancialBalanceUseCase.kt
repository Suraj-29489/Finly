package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.FinancialBalance
import com.personalexpensetracker.domain.model.FinancialPeriodSummary
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Use case to calculate the all-time, current-month, and today's financial balance and cash flow (Phase 8 Step 4).
 * Pure domain logic reacting to any additions, edits, or deletions in income and expense repositories.
 */
class GetFinancialBalanceUseCase(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) {

    operator fun invoke(): Flow<FinancialBalance> {
        return combine(
            incomeRepository.getAllIncomes(),
            expenseRepository.getAllExpenses()
        ) { incomes, expenses ->
            val today = LocalDate.now(clock)
            val currentYearMonth = YearMonth.from(today)

            // 1. All-time calculations
            val totalIncome = incomes.fold(BigDecimal.ZERO) { acc, inc -> acc + inc.amount }
            val totalExpenses = expenses.fold(BigDecimal.ZERO) { acc, exp -> acc + exp.amount }
            val netBalance = totalIncome.subtract(totalExpenses)

            // 2. Current-month calculations
            val monthIncomes = incomes.filter {
                val incomeDate = it.date.atZone(zoneId).toLocalDate()
                YearMonth.from(incomeDate) == currentYearMonth
            }
            val monthExpenses = expenses.filter {
                val expenseDate = it.date.atZone(zoneId).toLocalDate()
                YearMonth.from(expenseDate) == currentYearMonth
            }
            val monthIncomeTotal = monthIncomes.fold(BigDecimal.ZERO) { acc, inc -> acc + inc.amount }
            val monthExpenseTotal = monthExpenses.fold(BigDecimal.ZERO) { acc, exp -> acc + exp.amount }
            val monthCashFlow = monthIncomeTotal.subtract(monthExpenseTotal)

            val currentMonthSummary = FinancialPeriodSummary(
                totalIncome = monthIncomeTotal,
                totalExpenses = monthExpenseTotal,
                netCashFlow = monthCashFlow
            )

            // 3. Today's calculations
            val todayIncomes = incomes.filter {
                it.date.atZone(zoneId).toLocalDate() == today
            }
            val todayExpenses = expenses.filter {
                it.date.atZone(zoneId).toLocalDate() == today
            }
            val todayIncomeTotal = todayIncomes.fold(BigDecimal.ZERO) { acc, inc -> acc + inc.amount }
            val todayExpenseTotal = todayExpenses.fold(BigDecimal.ZERO) { acc, exp -> acc + exp.amount }
            val todayCashFlow = todayIncomeTotal.subtract(todayExpenseTotal)

            val todaySummary = FinancialPeriodSummary(
                totalIncome = todayIncomeTotal,
                totalExpenses = todayExpenseTotal,
                netCashFlow = todayCashFlow
            )

            FinancialBalance(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netBalance = netBalance,
                currentMonth = currentMonthSummary,
                today = todaySummary
            )
        }
    }
}

