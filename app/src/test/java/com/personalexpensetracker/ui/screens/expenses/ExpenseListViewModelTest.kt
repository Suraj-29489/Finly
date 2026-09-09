package com.personalexpensetracker.ui.screens.expenses

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.usecase.DeleteExpenseUseCase
import com.personalexpensetracker.domain.usecase.GetExpensesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(ExpenseListUiState.Empty, viewModel.uiState.value)
        collectJob.cancel()
    }

    @Test
    fun uiState_withExpenses_emitsSuccessState() = runTest(testDispatcher) {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Dinner",
                amount = BigDecimal("35.00"),
                category = "Food",
                date = Instant.now()
            )
        )
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        assertEquals(1, (state as ExpenseListUiState.Success).expenses.size)
        assertEquals("Dinner", state.expenses[0].title)
        collectJob.cancel()
    }

    @Test
    fun uiState_onRepositoryError_emitsErrorState() = runTest(testDispatcher) {
        val errorFlow: Flow<List<Expense>> = flow {
            throw RuntimeException("Database read error")
        }
        val repository = FakeExpenseRepository(errorFlow)
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Error)
        assertEquals("Database read error", (state as ExpenseListUiState.Error).message)
        collectJob.cancel()
    }

    @Test
    fun deleteExpense_requestAndCancel_doesNotDelete() = runTest(testDispatcher) {
        val item = Expense(id = 1L, title = "Item", amount = BigDecimal("10.00"), category = "Food", date = Instant.now())
        val repository = DynamicFakeExpenseRepository(mutableListOf(item))
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        viewModel.requestDelete(item)
        assertEquals(item, viewModel.pendingDeleteExpense.value)

        viewModel.cancelDelete()
        assertNull(viewModel.pendingDeleteExpense.value)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun deleteExpense_confirmed_deletesCorrectExpenseAndLeavesOthers() = runTest(testDispatcher) {
        val item1 = Expense(id = 1L, title = "Item 1", amount = BigDecimal("10.00"), category = "Food", date = Instant.now())
        val item2 = Expense(id = 2L, title = "Item 2", amount = BigDecimal("20.00"), category = "Food", date = Instant.now())
        val repository = DynamicFakeExpenseRepository(mutableListOf(item1, item2))
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.requestDelete(item1)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.pendingDeleteExpense.value)
        assertEquals(1, repository.items.size)
        assertEquals(2L, repository.items[0].id)
        assertEquals("Item 2", repository.items[0].title)

        collectJob.cancel()
    }

    @Test
    fun deleteExpense_lastExpense_resultsInEmptyState() = runTest(testDispatcher) {
        val item = Expense(id = 1L, title = "Sole Item", amount = BigDecimal("10.00"), category = "Food", date = Instant.now())
        val repository = DynamicFakeExpenseRepository(mutableListOf(item))
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ExpenseListUiState.Success)

        viewModel.requestDelete(item)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(repository.items.isEmpty())
        assertEquals(ExpenseListUiState.Empty, viewModel.uiState.value)

        collectJob.cancel()
    }

    @Test
    fun deleteExpense_failure_setsDeletionError() = runTest(testDispatcher) {
        val item = Expense(id = 1L, title = "Faulty Item", amount = BigDecimal("10.00"), category = "Food", date = Instant.now())
        val repository = object : ExpenseRepository {
            override fun getAllExpenses(): Flow<List<Expense>> = flowOf(listOf(item))
            override suspend fun getExpenseById(id: Long): Expense? = item
            override suspend fun insertExpense(expense: Expense): Long = 1L
            override suspend fun updateExpense(expense: Expense) {}
            override suspend fun deleteExpense(expense: Expense) { throw RuntimeException("Failed to delete") }
            override suspend fun deleteExpenseById(id: Long) { throw RuntimeException("Failed to delete") }
            override fun getExpensesByCategory(category: String): Flow<List<Expense>> = flowOf(emptyList())
            override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> = flowOf(emptyList())
            override fun getTotalExpensesInCents(): Flow<Long?> = flowOf(0L)
        }
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        val viewModel = ExpenseListViewModel(getUseCase, deleteUseCase)

        viewModel.requestDelete(item)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals("Failed to delete", viewModel.deletionError.value)
    }

    // --- Step 5 Tests: Search, Filter, Sort, Reset ---

    private val sampleExpense1 = Expense(
        id = 1L,
        title = "Morning Coffee",
        amount = BigDecimal("4.50"),
        category = "Food",
        date = Instant.parse("2026-09-07T08:00:00Z"),
        notes = "Cappuccino"
    )

    private val sampleExpense2 = Expense(
        id = 2L,
        title = "Business Lunch",
        amount = BigDecimal("45.00"),
        category = "Food",
        date = Instant.parse("2026-09-07T12:30:00Z"),
        notes = "With client"
    )

    private val sampleExpense3 = Expense(
        id = 3L,
        title = "Metro Pass",
        amount = BigDecimal("20.00"),
        category = "Transportation",
        date = Instant.parse("2026-09-01T10:00:00Z"),
        notes = "Monthly commute"
    )

    private val sampleExpense4 = Expense(
        id = 4L,
        title = "Old Book",
        amount = BigDecimal("15.00"),
        category = "Entertainment",
        date = Instant.parse("2026-08-15T14:00:00Z"),
        notes = "Novel"
    )

    private val allSampleExpenses = listOf(sampleExpense1, sampleExpense2, sampleExpense3, sampleExpense4)
    private val testUtcZone = ZoneId.of("UTC")
    private val testFixedClock = Clock.fixed(Instant.parse("2026-09-07T15:00:00Z"), testUtcZone)

    private fun createStep5ViewModel(expenses: List<Expense> = allSampleExpenses): ExpenseListViewModel {
        val repository = FakeExpenseRepository(flowOf(expenses))
        val getUseCase = GetExpensesUseCase(repository)
        val deleteUseCase = DeleteExpenseUseCase(repository)
        return ExpenseListViewModel(
            getExpensesUseCase = getUseCase,
            deleteExpenseUseCase = deleteUseCase,
            zoneId = testUtcZone,
            clock = testFixedClock
        )
    }

    @Test
    fun search_byDescription_matchesCorrectExpenses() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("coffee")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(1, items.size)
        assertEquals("Morning Coffee", items[0].title)

        collectJob.cancel()
    }

    @Test
    fun search_byCategory_matchesCorrectExpenses() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("transportation")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(1, items.size)
        assertEquals("Metro Pass", items[0].title)

        collectJob.cancel()
    }

    @Test
    fun filter_byCategory_matchesCorrectExpenses() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onCategorySelect("Food")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(2, items.size)
        assertTrue(items.all { it.category == "Food" })

        collectJob.cancel()
    }

    @Test
    fun filter_byDate_filtersTodayAndThisMonthCorrectly() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Filter Today (should match Sep 07 items: Morning Coffee and Business Lunch)
        viewModel.onDateFilterSelect(ExpenseDateFilter.TODAY)
        advanceUntilIdle()
        var state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        var items = (state as ExpenseListUiState.Success).expenses
        assertEquals(2, items.size)
        assertTrue(items.contains(sampleExpense1))
        assertTrue(items.contains(sampleExpense2))

        // Filter This Month (should match Sep 07 and Sep 01 items: 3 items)
        viewModel.onDateFilterSelect(ExpenseDateFilter.THIS_MONTH)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        items = (state as ExpenseListUiState.Success).expenses
        assertEquals(3, items.size)
        assertTrue(!items.contains(sampleExpense4))

        // Filter All Dates (should match all 4)
        viewModel.onDateFilterSelect(ExpenseDateFilter.ALL)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        items = (state as ExpenseListUiState.Success).expenses
        assertEquals(4, items.size)

        collectJob.cancel()
    }

    @Test
    fun sort_newestFirst_ordersByDateDescending() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortOrderSelect(ExpenseSortOrder.NEWEST_FIRST)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(4, items.size)
        assertEquals("Business Lunch", items[0].title)
        assertEquals("Morning Coffee", items[1].title)
        assertEquals("Metro Pass", items[2].title)
        assertEquals("Old Book", items[3].title)

        collectJob.cancel()
    }

    @Test
    fun sort_oldestFirst_ordersByDateAscending() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortOrderSelect(ExpenseSortOrder.OLDEST_FIRST)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(4, items.size)
        assertEquals("Old Book", items[0].title)
        assertEquals("Metro Pass", items[1].title)
        assertEquals("Morning Coffee", items[2].title)
        assertEquals("Business Lunch", items[3].title)

        collectJob.cancel()
    }

    @Test
    fun sort_highestAmount_ordersByAmountDescending() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortOrderSelect(ExpenseSortOrder.AMOUNT_HIGH_TO_LOW)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(4, items.size)
        assertEquals(BigDecimal("45.00"), items[0].amount)
        assertEquals(BigDecimal("20.00"), items[1].amount)
        assertEquals(BigDecimal("15.00"), items[2].amount)
        assertEquals(BigDecimal("4.50"), items[3].amount)

        collectJob.cancel()
    }

    @Test
    fun sort_lowestAmount_ordersByAmountAscending() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSortOrderSelect(ExpenseSortOrder.AMOUNT_LOW_TO_HIGH)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(4, items.size)
        assertEquals(BigDecimal("4.50"), items[0].amount)
        assertEquals(BigDecimal("15.00"), items[1].amount)
        assertEquals(BigDecimal("20.00"), items[2].amount)
        assertEquals(BigDecimal("45.00"), items[3].amount)

        collectJob.cancel()
    }

    @Test
    fun searchAndFilter_combined_filtersCorrectly() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("lunch")
        viewModel.onCategorySelect("Food")
        viewModel.onDateFilterSelect(ExpenseDateFilter.THIS_MONTH)
        viewModel.onSortOrderSelect(ExpenseSortOrder.AMOUNT_HIGH_TO_LOW)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        val items = (state as ExpenseListUiState.Success).expenses
        assertEquals(1, items.size)
        assertEquals("Business Lunch", items[0].title)

        collectJob.cancel()
    }

    @Test
    fun searchAndFilter_noMatches_emitsNoMatchingResultsState() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("nonexistent query 12345")
        advanceUntilIdle()

        assertEquals(ExpenseListUiState.NoMatchingResults, viewModel.uiState.value)

        collectJob.cancel()
    }

    @Test
    fun resetFilters_restoresCompleteListAndDefaultState() = runTest(testDispatcher) {
        val viewModel = createStep5ViewModel()
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSearchQueryChange("coffee")
        viewModel.onCategorySelect("Food")
        viewModel.onDateFilterSelect(ExpenseDateFilter.TODAY)
        viewModel.onSortOrderSelect(ExpenseSortOrder.AMOUNT_HIGH_TO_LOW)
        advanceUntilIdle()

        assertEquals(1, (viewModel.uiState.value as ExpenseListUiState.Success).expenses.size)
        assertTrue(viewModel.hasActiveFilters.value)

        viewModel.resetFilters()
        advanceUntilIdle()

        assertEquals("", viewModel.searchQuery.value)
        assertEquals("All", viewModel.selectedCategory.value)
        assertEquals(ExpenseDateFilter.ALL, viewModel.selectedDateFilter.value)
        assertEquals(ExpenseSortOrder.NEWEST_FIRST, viewModel.selectedSortOrder.value)
        assertEquals(false, viewModel.hasActiveFilters.value)

        val state = viewModel.uiState.value
        assertTrue(state is ExpenseListUiState.Success)
        assertEquals(4, (state as ExpenseListUiState.Success).expenses.size)

        collectJob.cancel()
    }

    private class FakeExpenseRepository(
        private val expensesFlow: Flow<List<Expense>>
    ) : ExpenseRepository {
        override fun getAllExpenses(): Flow<List<Expense>> = expensesFlow
        override suspend fun getExpenseById(id: Long): Expense? = null
        override suspend fun insertExpense(expense: Expense): Long = 1L
        override suspend fun updateExpense(expense: Expense) {}
        override suspend fun deleteExpense(expense: Expense) {}
        override suspend fun deleteExpenseById(id: Long) {}
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> = flowOf(emptyList())
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> = flowOf(emptyList())
        override fun getTotalExpensesInCents(): Flow<Long?> = flowOf(0L)
    }

    private class DynamicFakeExpenseRepository(
        val items: MutableList<Expense>
    ) : ExpenseRepository {
        private val flow = MutableStateFlow(items.toList())

        override fun getAllExpenses(): Flow<List<Expense>> = flow.asStateFlow()
        override suspend fun getExpenseById(id: Long): Expense? = items.find { it.id == id }
        override suspend fun insertExpense(expense: Expense): Long = 1L
        override suspend fun updateExpense(expense: Expense) {}
        override suspend fun deleteExpense(expense: Expense) {
            items.removeIf { it.id == expense.id }
            flow.value = items.toList()
        }
        override suspend fun deleteExpenseById(id: Long) {
            items.removeIf { it.id == id }
            flow.value = items.toList()
        }
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> = flowOf(emptyList())
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> = flowOf(emptyList())
        override fun getTotalExpensesInCents(): Flow<Long?> = flowOf(0L)
    }
}
