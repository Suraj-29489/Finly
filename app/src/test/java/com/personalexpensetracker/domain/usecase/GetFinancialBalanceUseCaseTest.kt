package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
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
import java.time.ZoneOffset

class GetFinancialBalanceUseCaseTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC)
    private val zoneId = ZoneOffset.UTC

    @Test
    fun `no income and no expenses returns all zero values`() = runTest {
        val expenseRepo = FakeExpenseRepo(emptyList())
        val incomeRepo = FakeIncomeRepo(emptyList())
        val useCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)

        val result = useCase().first()

        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal.ZERO, result.netBalance)
        assertEquals(BigDecimal.ZERO, result.currentMonth.netCashFlow)
        assertEquals(BigDecimal.ZERO, result.today.netCashFlow)
    }

    @Test
    fun `income only returns positive net balance and cash flows`() = runTest {
        val incomes = listOf(
            Income(
                id = 1L,
                title = "Salary",
                amount = BigDecimal("4000.00"),
                source = "Salary",
                date = Instant.parse("2026-09-07T10:00:00Z")
            )
        )
        val expenseRepo = FakeExpenseRepo(emptyList())
        val incomeRepo = FakeIncomeRepo(incomes)
        val useCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)

        val result = useCase().first()

        assertEquals(BigDecimal("4000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("4000.00"), result.netBalance)
        assertEquals(BigDecimal("4000.00"), result.currentMonth.totalIncome)
        assertEquals(BigDecimal("4000.00"), result.currentMonth.netCashFlow)
        assertEquals(BigDecimal("4000.00"), result.today.netCashFlow)
    }

    @Test
    fun `expenses only returns negative net balance and cash flows`() = runTest {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Rent",
                amount = BigDecimal("1500.00"),
                category = "Housing",
                date = Instant.parse("2026-09-07T10:00:00Z")
            )
        )
        val expenseRepo = FakeExpenseRepo(expenses)
        val incomeRepo = FakeIncomeRepo(emptyList())
        val useCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)

        val result = useCase().first()

        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal("1500.00"), result.totalExpenses)
        assertEquals(BigDecimal("-1500.00"), result.netBalance)
        assertEquals(BigDecimal("-1500.00"), result.currentMonth.netCashFlow)
        assertEquals(BigDecimal("-1500.00"), result.today.netCashFlow)
    }

    @Test
    fun `equal income and expenses returns zero net balance`() = runTest {
        val incomes = listOf(
            Income(
                id = 1L,
                title = "Side gig",
                amount = BigDecimal("500.00"),
                source = "Freelance",
                date = Instant.parse("2026-09-07T08:00:00Z")
            )
        )
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Groceries",
                amount = BigDecimal("500.00"),
                category = "Food",
                date = Instant.parse("2026-09-07T09:00:00Z")
            )
        )
        val expenseRepo = FakeExpenseRepo(expenses)
        val incomeRepo = FakeIncomeRepo(incomes)
        val useCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)

        val result = useCase().first()

        assertEquals(BigDecimal("500.00"), result.totalIncome)
        assertEquals(BigDecimal("500.00"), result.totalExpenses)
        assertEquals(BigDecimal("0.00"), result.netBalance)
        assertEquals(BigDecimal("0.00"), result.currentMonth.netCashFlow)
    }

    @Test
    fun `month and date isolation correctly attributes amounts`() = runTest {
        val incomes = listOf(
            // Today (Sep 07)
            Income(id = 1L, title = "Today Income", amount = BigDecimal("100.00"), source = "Gift", date = Instant.parse("2026-09-07T05:00:00Z")),
            // This month, earlier day (Sep 02)
            Income(id = 2L, title = "Earlier This Month", amount = BigDecimal("1000.00"), source = "Salary", date = Instant.parse("2026-09-02T10:00:00Z")),
            // Last month (August)
            Income(id = 3L, title = "August Income", amount = BigDecimal("500.00"), source = "Freelance", date = Instant.parse("2026-08-20T10:00:00Z"))
        )
        val expenses = listOf(
            // Today (Sep 07)
            Expense(id = 1L, title = "Today Lunch", amount = BigDecimal("25.00"), category = "Food", date = Instant.parse("2026-09-07T11:00:00Z")),
            // This month, earlier day (Sep 03)
            Expense(id = 2L, title = "Earlier Utility", amount = BigDecimal("75.00"), category = "Utilities", date = Instant.parse("2026-09-03T10:00:00Z")),
            // Last month (August)
            Expense(id = 3L, title = "August Expense", amount = BigDecimal("200.00"), category = "Shopping", date = Instant.parse("2026-08-10T10:00:00Z"))
        )
        val expenseRepo = FakeExpenseRepo(expenses)
        val incomeRepo = FakeIncomeRepo(incomes)
        val useCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)

        val result = useCase().first()

        // All time: Income = 1600.00, Expenses = 300.00, Net = 1300.00
        assertEquals(BigDecimal("1600.00"), result.totalIncome)
        assertEquals(BigDecimal("300.00"), result.totalExpenses)
        assertEquals(BigDecimal("1300.00"), result.netBalance)

        // Current month (Sep): Income = 1100.00, Expenses = 100.00, Cash Flow = 1000.00
        assertEquals(BigDecimal("1100.00"), result.currentMonth.totalIncome)
        assertEquals(BigDecimal("100.00"), result.currentMonth.totalExpenses)
        assertEquals(BigDecimal("1000.00"), result.currentMonth.netCashFlow)

        // Today (Sep 07): Income = 100.00, Expenses = 25.00, Cash Flow = 75.00
        assertEquals(BigDecimal("100.00"), result.today.totalIncome)
        assertEquals(BigDecimal("25.00"), result.today.totalExpenses)
        assertEquals(BigDecimal("75.00"), result.today.netCashFlow)
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

