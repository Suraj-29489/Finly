package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.CashFlowAnalyticsReport
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.model.FinancialPeriodSummary
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.MonthlyCashFlowTrend
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Use case to generate cash flow reporting and historical monthly trends for analytics (Phase 8 Step 6).
 */
class GetCashFlowReportUseCase(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) {

    operator fun invoke(
        period: FinancialPeriod = FinancialPeriod.THIS_MONTH,
        trendMonthsCount: Int = 6
    ): Flow<CashFlowAnalyticsReport> {
        return combine(
            incomeRepository.getAllIncomes(),
            expenseRepository.getAllExpenses()
        ) { incomes, expenses ->
            val today = LocalDate.now(clock)
            val currentYearMonth = YearMonth.from(today)

            val (filteredIncomes, filteredExpenses) = filterByPeriod(
                incomes = incomes,
                expenses = expenses,
                period = period,
                today = today,
                currentYearMonth = currentYearMonth,
                zoneId = zoneId
            )

            val totalIncome = filteredIncomes.fold(BigDecimal.ZERO) { acc, inc -> acc + inc.amount }
            val totalExpenses = filteredExpenses.fold(BigDecimal.ZERO) { acc, exp -> acc + exp.amount }
            val netCashFlow = totalIncome.subtract(totalExpenses)

            val summary = FinancialPeriodSummary(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netCashFlow = netCashFlow
            )

            // Calculate historical monthly trends for the past `trendMonthsCount` months
            // with zero-activity months filled in cleanly
            val monthlyTrends = (0 until trendMonthsCount).map { offset ->
                val ym = currentYearMonth.minusMonths((trendMonthsCount - 1 - offset).toLong())
                val mIncomes = incomes.filter {
                    YearMonth.from(it.date.atZone(zoneId).toLocalDate()) == ym
                }
                val mExpenses = expenses.filter {
                    YearMonth.from(it.date.atZone(zoneId).toLocalDate()) == ym
                }
                val mIncomeTotal = mIncomes.fold(BigDecimal.ZERO) { acc, i -> acc + i.amount }
                val mExpenseTotal = mExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
                MonthlyCashFlowTrend(
                    yearMonth = ym,
                    income = mIncomeTotal,
                    expenses = mExpenseTotal,
                    netCashFlow = mIncomeTotal.subtract(mExpenseTotal)
                )
            }

            CashFlowAnalyticsReport(
                period = period,
                summary = summary,
                monthlyTrends = monthlyTrends,
                incomeTransactionCount = filteredIncomes.size,
                expenseTransactionCount = filteredExpenses.size
            )
        }
    }

    private fun filterByPeriod(
        incomes: List<Income>,
        expenses: List<Expense>,
        period: FinancialPeriod,
        today: LocalDate,
        currentYearMonth: YearMonth,
        zoneId: ZoneId
    ): Pair<List<Income>, List<Expense>> {
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastMonth = currentYearMonth.minusMonths(1)

        val incomePredicate: (Income) -> Boolean = { inc ->
            val date = inc.date.atZone(zoneId).toLocalDate()
            when (period) {
                FinancialPeriod.TODAY -> date == today
                FinancialPeriod.THIS_WEEK -> !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                FinancialPeriod.THIS_MONTH -> YearMonth.from(date) == currentYearMonth
                FinancialPeriod.LAST_MONTH -> YearMonth.from(date) == lastMonth
                FinancialPeriod.THIS_YEAR -> date.year == today.year
                FinancialPeriod.ALL_TIME -> true
            }
        }

        val expensePredicate: (Expense) -> Boolean = { exp ->
            val date = exp.date.atZone(zoneId).toLocalDate()
            when (period) {
                FinancialPeriod.TODAY -> date == today
                FinancialPeriod.THIS_WEEK -> !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                FinancialPeriod.THIS_MONTH -> YearMonth.from(date) == currentYearMonth
                FinancialPeriod.LAST_MONTH -> YearMonth.from(date) == lastMonth
                FinancialPeriod.THIS_YEAR -> date.year == today.year
                FinancialPeriod.ALL_TIME -> true
            }
        }

        return Pair(
            incomes.filter(incomePredicate),
            expenses.filter(expensePredicate)
        )
    }
}

