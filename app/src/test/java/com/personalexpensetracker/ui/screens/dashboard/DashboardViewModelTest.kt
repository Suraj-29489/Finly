package com.personalexpensetracker.ui.screens.dashboard

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.usecase.GetExpensesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneOffset.UTC
    // Fixed reference time: Sep 15, 2026, 12:00:00 UTC
    private val fixedNow = Instant.parse("2026-09-15T12:00:00Z")
    private val fixedClock = Clock.fixed(fixedNow, zoneId)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_emptyDatabase_emitsEmptyState() = runTest(testDispatcher) {
        val repository = FakeExpenseRepository(flowOf(emptyList()))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(DashboardUiState.Empty, viewModel.uiState.value)
        collectJob.cancel()
    }

    @Test
    fun uiState_currentMonthTotal_calculatedCorrectly() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Groceries",
                amount = BigDecimal("50.00"),
                category = "Food",
                date = Instant.parse("2026-09-02T10:00:00Z")
            ),
            Expense(
                id = 2L,
                title = "Dinner",
                amount = BigDecimal("30.50"),
                category = "Food",
                date = Instant.parse("2026-09-14T18:00:00Z")
            ),
            Expense(
                id = 3L,
                title = "August Rent",
                amount = BigDecimal("850.00"),
                category = "Utilities",
                date = Instant.parse("2026-08-31T23:59:59Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        // Sep 02 ($50.00) + Sep 14 ($30.50) = $80.50 (August excluded)
        assertEquals(BigDecimal("80.50"), success.currentMonthTotal)
        collectJob.cancel()
    }

    @Test
    fun uiState_todayTotal_calculatedCorrectly() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Morning Coffee",
                amount = BigDecimal("4.75"),
                category = "Food",
                date = Instant.parse("2026-09-15T08:30:00Z") // Today
            ),
            Expense(
                id = 2L,
                title = "Lunch",
                amount = BigDecimal("15.25"),
                category = "Food",
                date = Instant.parse("2026-09-15T12:30:00Z") // Today
            ),
            Expense(
                id = 3L,
                title = "Yesterday Movie",
                amount = BigDecimal("20.00"),
                category = "Entertainment",
                date = Instant.parse("2026-09-14T20:00:00Z") // Yesterday
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        // $4.75 + $15.25 = $20.00
        assertEquals(BigDecimal("20.00"), success.todayTotal)
        collectJob.cancel()
    }

    @Test
    fun uiState_currentMonthTransactionCount_isCorrect() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Item 1",
                amount = BigDecimal("10.00"),
                category = "Shopping",
                date = Instant.parse("2026-09-01T10:00:00Z")
            ),
            Expense(
                id = 2L,
                title = "Item 2",
                amount = BigDecimal("20.00"),
                category = "Shopping",
                date = Instant.parse("2026-09-10T10:00:00Z")
            ),
            Expense(
                id = 3L,
                title = "Item 3",
                amount = BigDecimal("30.00"),
                category = "Shopping",
                date = Instant.parse("2026-09-15T10:00:00Z")
            ),
            Expense(
                id = 4L,
                title = "July Old Item",
                amount = BigDecimal("40.00"),
                category = "Shopping",
                date = Instant.parse("2026-07-20T10:00:00Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        assertEquals(3, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    @Test
    fun uiState_recentTransactions_orderedCorrectlyAndCappedAtFive() = runTest(testDispatcher) {
        val expenses = (1..7).map { index ->
            Expense(
                id = index.toLong(),
                title = "Expense #$index",
                amount = BigDecimal("${index * 10}.00"),
                category = "General",
                date = Instant.parse("2026-09-0${index}T10:00:00Z")
            )
        }
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        // Capped at 5
        assertEquals(5, success.recentTransactions.size)

        // Newest first: 7, 6, 5, 4, 3
        assertEquals(7L, success.recentTransactions[0].id)
        assertEquals(6L, success.recentTransactions[1].id)
        assertEquals(5L, success.recentTransactions[2].id)
        assertEquals(4L, success.recentTransactions[3].id)
        assertEquals(3L, success.recentTransactions[4].id)

        collectJob.cancel()
    }

    @Test
    fun uiState_reactsToExpenseChanges_updatesDashboard() = runTest(testDispatcher) {
        val expenseFlow = MutableStateFlow<List<Expense>>(
            listOf(
                Expense(
                    id = 1L,
                    title = "Initial Expense",
                    amount = BigDecimal("25.00"),
                    category = "Food",
                    date = Instant.parse("2026-09-15T09:00:00Z") // Today
                )
            )
        )
        val repository = FakeExpenseRepository(expenseFlow.asStateFlow())
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        var state = viewModel.uiState.value as DashboardUiState.Success
        assertEquals(BigDecimal("25.00"), state.todayTotal)
        assertEquals(BigDecimal("25.00"), state.currentMonthTotal)
        assertEquals(1, state.currentMonthTransactionCount)
        assertEquals(1, state.recentTransactions.size)

        // Now simulate adding a second expense today
        val updatedList = expenseFlow.value + Expense(
            id = 2L,
            title = "Added Expense",
            amount = BigDecimal("15.00"),
            category = "Utilities",
            date = Instant.parse("2026-09-15T11:00:00Z")
        )
        expenseFlow.value = updatedList
        advanceUntilIdle()

        state = viewModel.uiState.value as DashboardUiState.Success
        assertEquals(BigDecimal("40.00"), state.todayTotal)
        assertEquals(BigDecimal("40.00"), state.currentMonthTotal)
        assertEquals(2, state.currentMonthTransactionCount)
        assertEquals(2, state.recentTransactions.size)

        // Now simulate deleting all expenses
        expenseFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals(DashboardUiState.Empty, viewModel.uiState.value)
        collectJob.cancel()
    }

    @Test
    fun uiState_multipleSameDayExpenses_aggregatesCorrectly() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Coffee",
                amount = BigDecimal("12.34"),
                category = "Food",
                date = Instant.parse("2026-09-15T08:00:00Z")
            ),
            Expense(
                id = 2L,
                title = "Lunch",
                amount = BigDecimal("5.66"),
                category = "Food",
                date = Instant.parse("2026-09-15T13:00:00Z")
            ),
            Expense(
                id = 3L,
                title = "Supplies",
                amount = BigDecimal("100.00"),
                category = "Office",
                date = Instant.parse("2026-09-15T16:00:00Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        // 12.34 + 5.66 + 100.00 = 118.00
        assertEquals(BigDecimal("118.00"), success.todayTotal)
        assertEquals(BigDecimal("118.00"), success.currentMonthTotal)
        assertEquals(3, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    @Test
    fun uiState_differentDatesAcrossCurrentMonth_aggregatesCorrectly() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "First of Month",
                amount = BigDecimal("10.00"),
                category = "General",
                date = Instant.parse("2026-09-01T00:00:00Z")
            ),
            Expense(
                id = 2L,
                title = "Mid Month",
                amount = BigDecimal("25.50"),
                category = "General",
                date = Instant.parse("2026-09-10T14:00:00Z")
            ),
            Expense(
                id = 3L,
                title = "Today Expense",
                amount = BigDecimal("14.50"),
                category = "Food",
                date = Instant.parse("2026-09-15T10:00:00Z") // Today
            ),
            Expense(
                id = 4L,
                title = "End of Month",
                amount = BigDecimal("50.00"),
                category = "Utilities",
                date = Instant.parse("2026-09-30T23:59:59Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        // Month total: 10.00 + 25.50 + 14.50 + 50.00 = 100.00
        assertEquals(BigDecimal("100.00"), success.currentMonthTotal)
        // Today total: only 14.50
        assertEquals(BigDecimal("14.50"), success.todayTotal)
        assertEquals(4, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    @Test
    fun uiState_expensesExistButNoneInCurrentMonth_returnsZeroMonthAndTodayTotals() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "August Gas",
                amount = BigDecimal("45.00"),
                category = "Transportation",
                date = Instant.parse("2026-08-20T10:00:00Z")
            ),
            Expense(
                id = 2L,
                title = "July Dinner",
                amount = BigDecimal("80.00"),
                category = "Food",
                date = Instant.parse("2026-07-15T19:00:00Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        assertEquals(BigDecimal("0.00"), success.currentMonthTotal)
        assertEquals(BigDecimal("0.00"), success.todayTotal)
        assertEquals(0, success.currentMonthTransactionCount)
        // Recent transactions still lists past items
        assertEquals(2, success.recentTransactions.size)
        collectJob.cancel()
    }

    @Test
    fun uiState_expensesInCurrentMonthButNoneToday_returnsZeroTodayTotal() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Sep 5 Expense",
                amount = BigDecimal("40.00"),
                category = "Utilities",
                date = Instant.parse("2026-09-05T10:00:00Z")
            ),
            Expense(
                id = 2L,
                title = "Sep 10 Expense",
                amount = BigDecimal("60.00"),
                category = "Food",
                date = Instant.parse("2026-09-10T14:00:00Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        assertEquals(BigDecimal("100.00"), success.currentMonthTotal)
        assertEquals(BigDecimal("0.00"), success.todayTotal)
        assertEquals(2, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    @Test
    fun uiState_editedExpenseAmountAndDate_updatesTotalsAccurately() = runTest(testDispatcher) {
        val expenseFlow = MutableStateFlow(
            listOf(
                Expense(
                    id = 1L,
                    title = "Editable Expense",
                    amount = BigDecimal("50.00"),
                    category = "Food",
                    date = Instant.parse("2026-09-15T09:00:00Z") // Today
                )
            )
        )
        val repository = FakeExpenseRepository(expenseFlow.asStateFlow())
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        var success = viewModel.uiState.value as DashboardUiState.Success
        assertEquals(BigDecimal("50.00"), success.todayTotal)
        assertEquals(BigDecimal("50.00"), success.currentMonthTotal)
        assertEquals(1, success.currentMonthTransactionCount)

        // Edit expense: amount changed to 75.50 and date moved to yesterday (Sep 14)
        expenseFlow.value = listOf(
            Expense(
                id = 1L,
                title = "Editable Expense",
                amount = BigDecimal("75.50"),
                category = "Food",
                date = Instant.parse("2026-09-14T15:00:00Z") // Yesterday
            )
        )
        advanceUntilIdle()

        success = viewModel.uiState.value as DashboardUiState.Success
        assertEquals(BigDecimal("0.00"), success.todayTotal)
        assertEquals(BigDecimal("75.50"), success.currentMonthTotal)
        assertEquals(1, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    @Test
    fun uiState_deletedExpense_decrementsTotalsAndCount() = runTest(testDispatcher) {
        val expenseFlow = MutableStateFlow(
            listOf(
                Expense(
                    id = 1L,
                    title = "Expense 1",
                    amount = BigDecimal("30.00"),
                    category = "Food",
                    date = Instant.parse("2026-09-15T09:00:00Z") // Today
                ),
                Expense(
                    id = 2L,
                    title = "Expense 2",
                    amount = BigDecimal("20.00"),
                    category = "Utilities",
                    date = Instant.parse("2026-09-15T11:00:00Z") // Today
                )
            )
        )
        val repository = FakeExpenseRepository(expenseFlow.asStateFlow())
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, zoneId, fixedClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        var success = viewModel.uiState.value as DashboardUiState.Success
        assertEquals(BigDecimal("50.00"), success.todayTotal)
        assertEquals(BigDecimal("50.00"), success.currentMonthTotal)
        assertEquals(2, success.currentMonthTransactionCount)

        // Delete Expense 2
        expenseFlow.value = listOf(expenseFlow.value.first())
        advanceUntilIdle()

        success = viewModel.uiState.value as DashboardUiState.Success
        assertEquals(BigDecimal("30.00"), success.todayTotal)
        assertEquals(BigDecimal("30.00"), success.currentMonthTotal)
        assertEquals(1, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    @Test
    fun uiState_timezoneOffset_respectsDeviceLocalDateAndMonth() = runTest(testDispatcher) {
        // Reference time: 2026-09-30 23:00:00 UTC
        // In UTC: Sep 30, 2026 (Month: September)
        // In Asia/Kolkata (UTC+5:30): Oct 01, 2026 04:30:00 (Month: October)
        val customZoneId = ZoneId.of("Asia/Kolkata")
        val referenceInstant = Instant.parse("2026-09-30T23:00:00Z")
        val kolkataClock = Clock.fixed(referenceInstant, customZoneId)

        val expenses = listOf(
            // Expense A: 2026-09-30T23:00:00Z -> In Kolkata, this is Oct 01 04:30:00 (Today & Current Month: October)
            Expense(
                id = 1L,
                title = "Kolkata Oct 1",
                amount = BigDecimal("50.00"),
                category = "Food",
                date = Instant.parse("2026-09-30T23:00:00Z")
            ),
            // Expense B: 2026-09-30T12:00:00Z -> In Kolkata, this is Sep 30 17:30:00 (Previous Month: September)
            Expense(
                id = 2L,
                title = "Kolkata Sep 30",
                amount = BigDecimal("100.00"),
                category = "Bills",
                date = Instant.parse("2026-09-30T12:00:00Z")
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getExpensesUseCase = GetExpensesUseCase(repository)
        val viewModel = DashboardViewModel(getExpensesUseCase, customZoneId, kolkataClock)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success

        // Current month in Kolkata is October! Only Expense A is in October.
        assertEquals(BigDecimal("50.00"), success.currentMonthTotal)
        // Today in Kolkata is Oct 1! Only Expense A is Oct 1.
        assertEquals(BigDecimal("50.00"), success.todayTotal)
        assertEquals(1, success.currentMonthTransactionCount)
        collectJob.cancel()
    }

    private class FakeExpenseRepository(
        private val flow: Flow<List<Expense>>
    ) : ExpenseRepository {
        override fun getAllExpenses(): Flow<List<Expense>> = flow
        override suspend fun getExpenseById(id: Long): Expense? = null
        override suspend fun insertExpense(expense: Expense): Long = 1L
        override suspend fun updateExpense(expense: Expense) {}
        override suspend fun deleteExpense(expense: Expense) {}
        override suspend fun deleteExpenseById(id: Long) {}
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> = flowOf(emptyList())
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> = flowOf(emptyList())
        override fun getTotalExpensesInCents(): Flow<Long?> = flowOf(0L)
    }
}

