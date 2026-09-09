package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class GetCashFlowReportUseCaseTest {

    // 2026-09-07 is Monday
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC)
    private val zoneId = ZoneOffset.UTC

    @Test
    fun `empty period returns zero summary and correct trends with zero activity`() = runTest {
        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(emptyList()),
            FakeIncomeRepo(emptyList()),
            zoneId,
            fixedClock
        )

        val report = useCase(FinancialPeriod.THIS_MONTH, trendMonthsCount = 6).first()

        assertEquals(BigDecimal.ZERO, report.summary.totalIncome)
        assertEquals(BigDecimal.ZERO, report.summary.totalExpenses)
        assertEquals(BigDecimal.ZERO, report.summary.netCashFlow)
        assertEquals(0, report.incomeTransactionCount)
        assertEquals(0, report.expenseTransactionCount)
        assertEquals(6, report.monthlyTrends.size)
        // Last trend should be current month (Sep 2026)
        assertEquals(YearMonth.of(2026, 9), report.monthlyTrends.last().yearMonth)
        assertEquals(BigDecimal.ZERO, report.monthlyTrends.last().netCashFlow)
    }

    @Test
    fun `income-only period calculates positive net cash flow`() = runTest {
        val incomes = listOf(
            Income(1L, "Salary", BigDecimal("3000.00"), "Salary", Instant.parse("2026-09-07T08:00:00Z"))
        )
        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(emptyList()),
            FakeIncomeRepo(incomes),
            zoneId,
            fixedClock
        )

        val report = useCase(FinancialPeriod.TODAY).first()

        assertEquals(BigDecimal("3000.00"), report.summary.totalIncome)
        assertEquals(BigDecimal.ZERO, report.summary.totalExpenses)
        assertEquals(BigDecimal("3000.00"), report.summary.netCashFlow)
        assertEquals(1, report.incomeTransactionCount)
        assertEquals(0, report.expenseTransactionCount)
    }

    @Test
    fun `expense-only period calculates negative net cash flow`() = runTest {
        val expenses = listOf(
            Expense(1L, "Groceries", BigDecimal("150.00"), "Food", Instant.parse("2026-09-07T10:00:00Z"))
        )
        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(expenses),
            FakeIncomeRepo(emptyList()),
            zoneId,
            fixedClock
        )

        val report = useCase(FinancialPeriod.TODAY).first()

        assertEquals(BigDecimal.ZERO, report.summary.totalIncome)
        assertEquals(BigDecimal("150.00"), report.summary.totalExpenses)
        assertEquals(BigDecimal("-150.00"), report.summary.netCashFlow)
        assertEquals(0, report.incomeTransactionCount)
        assertEquals(1, report.expenseTransactionCount)
    }

    @Test
    fun `mixed period calculates correct net cash flow`() = runTest {
        val incomes = listOf(
            Income(1L, "Freelance", BigDecimal("500.00"), "Freelance", Instant.parse("2026-09-07T09:00:00Z")),
            Income(2L, "Bonus", BigDecimal("200.00"), "Salary", Instant.parse("2026-09-07T11:00:00Z"))
        )
        val expenses = listOf(
            Expense(1L, "Utilities", BigDecimal("300.00"), "Utilities", Instant.parse("2026-09-07T10:00:00Z"))
        )
        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(expenses),
            FakeIncomeRepo(incomes),
            zoneId,
            fixedClock
        )

        val report = useCase(FinancialPeriod.THIS_MONTH).first()

        assertEquals(BigDecimal("700.00"), report.summary.totalIncome)
        assertEquals(BigDecimal("300.00"), report.summary.totalExpenses)
        assertEquals(BigDecimal("400.00"), report.summary.netCashFlow)
        assertEquals(2, report.incomeTransactionCount)
        assertEquals(1, report.expenseTransactionCount)
    }

    @Test
    fun `period boundaries test previous month vs current month isolation`() = runTest {
        val augIncome = Income(1L, "August Pay", BigDecimal("2500.00"), "Salary", Instant.parse("2026-08-25T12:00:00Z"))
        val sepIncome = Income(2L, "September Pay", BigDecimal("2800.00"), "Salary", Instant.parse("2026-09-05T12:00:00Z"))
        val augExpense = Expense(1L, "Rent", BigDecimal("800.00"), "Housing", Instant.parse("2026-08-28T12:00:00Z"))
        val sepExpense = Expense(2L, "Rent", BigDecimal("900.00"), "Housing", Instant.parse("2026-09-02T12:00:00Z"))

        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(listOf(augExpense, sepExpense)),
            FakeIncomeRepo(listOf(augIncome, sepIncome)),
            zoneId,
            fixedClock
        )

        // Test LAST_MONTH (August 2026)
        val lastMonthReport = useCase(FinancialPeriod.LAST_MONTH).first()
        assertEquals(BigDecimal("2500.00"), lastMonthReport.summary.totalIncome)
        assertEquals(BigDecimal("800.00"), lastMonthReport.summary.totalExpenses)
        assertEquals(BigDecimal("1700.00"), lastMonthReport.summary.netCashFlow)

        // Test THIS_MONTH (September 2026)
        val thisMonthReport = useCase(FinancialPeriod.THIS_MONTH).first()
        assertEquals(BigDecimal("2800.00"), thisMonthReport.summary.totalIncome)
        assertEquals(BigDecimal("900.00"), thisMonthReport.summary.totalExpenses)
        assertEquals(BigDecimal("1900.00"), thisMonthReport.summary.netCashFlow)

        // Test THIS_WEEK (Sep 7 is Monday, so Aug is outside)
        val thisWeekReport = useCase(FinancialPeriod.THIS_WEEK).first()
        assertEquals(BigDecimal.ZERO, thisWeekReport.summary.totalIncome)
        assertEquals(BigDecimal.ZERO, thisWeekReport.summary.totalExpenses)
    }

    @Test
    fun `month and year transition trends populated across 6 months correctly`() = runTest {
        // Today is 2026-09-07, 6 months trends are: Apr 2026, May 2026, Jun 2026, Jul 2026, Aug 2026, Sep 2026
        val augIncome = Income(1L, "Aug Income", BigDecimal("1000.00"), "Salary", Instant.parse("2026-08-15T12:00:00Z"))
        val sepExpense = Expense(1L, "Shopping", BigDecimal("200.00"), "Shopping", Instant.parse("2026-09-03T12:00:00Z"))

        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(listOf(sepExpense)),
            FakeIncomeRepo(listOf(augIncome)),
            zoneId,
            fixedClock
        )

        val report = useCase(FinancialPeriod.THIS_YEAR, trendMonthsCount = 6).first()
        val trends = report.monthlyTrends
        assertEquals(6, trends.size)

        val augTrend = trends.find { it.yearMonth == YearMonth.of(2026, 8) }!!
        assertEquals(BigDecimal("1000.00"), augTrend.income)
        assertEquals(BigDecimal.ZERO, augTrend.expenses)
        assertEquals(BigDecimal("1000.00"), augTrend.netCashFlow)

        val sepTrend = trends.find { it.yearMonth == YearMonth.of(2026, 9) }!!
        assertEquals(BigDecimal.ZERO, sepTrend.income)
        assertEquals(BigDecimal("200.00"), sepTrend.expenses)
        assertEquals(BigDecimal("-200.00"), sepTrend.netCashFlow)
    }

    private class FakeExpenseRepo(private val list: List<Expense>) : ExpenseRepository {
        override fun getAllExpenses(): Flow<List<Expense>> = MutableStateFlow(list)
        override suspend fun getExpenseById(id: Long): Expense? = list.find { it.id == id }
        override suspend fun insertExpense(expense: Expense): Long = 1L
        override suspend fun updateExpense(expense: Expense) {}
        override suspend fun deleteExpense(expense: Expense) {}
        override suspend fun deleteExpenseById(id: Long) {}
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> = MutableStateFlow(emptyList())
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> =
            MutableStateFlow(list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
        override fun getTotalExpensesInCents(): Flow<Long?> = MutableStateFlow(0L)
    }

    private class FakeIncomeRepo(private val list: List<Income>) : IncomeRepository {
        override fun getAllIncomes(): Flow<List<Income>> = MutableStateFlow(list)
        override suspend fun getIncomeById(id: Long): Income? = list.find { it.id == id }
        override suspend fun insertIncome(income: Income): Long = 1L
        override suspend fun updateIncome(income: Income) {}
        override suspend fun deleteIncome(income: Income) {}
        override suspend fun deleteIncomeById(id: Long) {}
        override fun getIncomesBySource(source: String): Flow<List<Income>> = MutableStateFlow(emptyList())
        override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> =
            MutableStateFlow(list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
        override fun getTotalIncomeInCents(): Flow<Long?> = MutableStateFlow(0L)
    }
}

