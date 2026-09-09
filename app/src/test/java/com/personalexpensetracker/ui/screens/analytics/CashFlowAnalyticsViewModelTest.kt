package com.personalexpensetracker.ui.screens.analytics

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import com.personalexpensetracker.domain.usecase.GetCashFlowReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CashFlowAnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC)
    private val zoneId = ZoneOffset.UTC

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads this month cash flow report`() = runTest {
        val incomes = listOf(
            Income(1L, "Salary", BigDecimal("5000.00"), "Salary", Instant.parse("2026-09-05T10:00:00Z"))
        )
        val expenses = listOf(
            Expense(1L, "Rent", BigDecimal("1500.00"), "Housing", Instant.parse("2026-09-02T10:00:00Z"))
        )
        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(expenses),
            FakeIncomeRepo(incomes),
            zoneId,
            fixedClock
        )

        val viewModel = CashFlowAnalyticsViewModel(useCase)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(FinancialPeriod.THIS_MONTH, state.selectedPeriod)
        assertEquals(BigDecimal("5000.00"), state.summary.totalIncome)
        assertEquals(BigDecimal("1500.00"), state.summary.totalExpenses)
        assertEquals(BigDecimal("3500.00"), state.summary.netCashFlow)
        assertEquals(1, state.incomeCount)
        assertEquals(1, state.expenseCount)

        job.cancel()
    }

    @Test
    fun `selecting new period updates selectedPeriod and fetches updated report`() = runTest {
        val incomes = listOf(
            // September (this month, not today)
            Income(1L, "Month Salary", BigDecimal("1000.00"), "Salary", Instant.parse("2026-09-01T10:00:00Z")),
            // Today (Sep 07)
            Income(2L, "Today Sale", BigDecimal("100.00"), "Business", Instant.parse("2026-09-07T10:00:00Z"))
        )
        val expenses = listOf(
            // Today (Sep 07)
            Expense(1L, "Today Coffee", BigDecimal("20.00"), "Food", Instant.parse("2026-09-07T11:00:00Z")),
            // Earlier this month
            Expense(2L, "Groceries", BigDecimal("200.00"), "Food", Instant.parse("2026-09-03T10:00:00Z"))
        )
        val useCase = GetCashFlowReportUseCase(
            FakeExpenseRepo(expenses),
            FakeIncomeRepo(incomes),
            zoneId,
            fixedClock
        )

        val viewModel = CashFlowAnalyticsViewModel(useCase)
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Initially THIS_MONTH
        assertEquals(FinancialPeriod.THIS_MONTH, viewModel.uiState.value.selectedPeriod)
        assertEquals(BigDecimal("1100.00"), viewModel.uiState.value.summary.totalIncome)

        // Select TODAY
        viewModel.onPeriodSelected(FinancialPeriod.TODAY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(FinancialPeriod.TODAY, state.selectedPeriod)
        assertEquals(BigDecimal("100.00"), state.summary.totalIncome)
        assertEquals(BigDecimal("20.00"), state.summary.totalExpenses)
        assertEquals(BigDecimal("80.00"), state.summary.netCashFlow)

        job.cancel()
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

