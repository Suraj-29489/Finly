package com.personalexpensetracker.ui.screens.income

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository
import com.personalexpensetracker.domain.usecase.DeleteIncomeUseCase
import com.personalexpensetracker.domain.usecase.GetIncomesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-07T10:00:00Z"), ZoneOffset.UTC)
    private val zoneId = ZoneOffset.UTC

    private lateinit var fakeRepository: FakeIncomeRepository
    private lateinit var getIncomesUseCase: GetIncomesUseCase
    private lateinit var deleteIncomeUseCase: DeleteIncomeUseCase
    private lateinit var viewModel: IncomeListViewModel

    private val sampleIncomes = listOf(
        Income(
            id = 1L,
            title = "Monthly Salary",
            amount = BigDecimal("5000.00"),
            source = "Salary",
            date = Instant.parse("2026-09-01T10:00:00Z"),
            notes = "Direct deposit"
        ),
        Income(
            id = 2L,
            title = "Web Development Freelance",
            amount = BigDecimal("1200.00"),
            source = "Freelance",
            date = Instant.parse("2026-09-07T08:00:00Z"),
            notes = "Client project"
        ),
        Income(
            id = 3L,
            title = "Dividend Payout",
            amount = BigDecimal("300.00"),
            source = "Investments",
            date = Instant.parse("2026-08-15T10:00:00Z"),
            notes = "Q2 dividend"
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeIncomeRepository(sampleIncomes.toMutableList())
        getIncomesUseCase = GetIncomesUseCase(fakeRepository)
        deleteIncomeUseCase = DeleteIncomeUseCase(fakeRepository)
        viewModel = IncomeListViewModel(
            getIncomesUseCase = getIncomesUseCase,
            deleteIncomeUseCase = deleteIncomeUseCase,
            zoneId = zoneId,
            clock = fixedClock
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits Success with sorted incomes and total inflow`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(3, success.incomes.size)
        // Default sort is newest first
        assertEquals("Web Development Freelance", success.incomes[0].title)
        assertEquals(BigDecimal("6500.00"), success.totalInflow)
        collectJob.cancel()
    }

    @Test
    fun `search query filters by title, source, or notes`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onSearchQueryChange("dividend")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(1, success.incomes.size)
        assertEquals("Dividend Payout", success.incomes[0].title)
        collectJob.cancel()
    }

    @Test
    fun `source filter correctly restricts to selected source`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onSourceSelect("Freelance")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(1, success.incomes.size)
        assertEquals("Web Development Freelance", success.incomes[0].title)
        collectJob.cancel()
    }

    @Test
    fun `date filter THIS_MONTH excludes past months`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onDateFilterSelect(IncomeDateFilter.THIS_MONTH)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(2, success.incomes.size) // August dividend excluded
        collectJob.cancel()
    }

    @Test
    fun `date filter TODAY includes only today incomes`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onDateFilterSelect(IncomeDateFilter.TODAY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(1, success.incomes.size)
        assertEquals(2L, success.incomes[0].id)
        collectJob.cancel()
    }

    @Test
    fun `sort order HIGHEST_AMOUNT sorts descending by amount`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onSortOrderSelect(IncomeSortOrder.HIGHEST_AMOUNT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(BigDecimal("5000.00"), success.incomes[0].amount)
        assertEquals(BigDecimal("1200.00"), success.incomes[1].amount)
        assertEquals(BigDecimal("300.00"), success.incomes[2].amount)
        collectJob.cancel()
    }

    @Test
    fun `confirm delete removes income from list and repository`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val toDelete = sampleIncomes[0]
        viewModel.onRequestDelete(toDelete)
        assertEquals(toDelete, viewModel.pendingDeleteIncome.value)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.pendingDeleteIncome.value)
        val state = viewModel.uiState.value
        assertTrue(state is IncomeListUiState.Success)
        val success = state as IncomeListUiState.Success
        assertEquals(2, success.incomes.size)
        assertTrue(success.incomes.none { it.id == toDelete.id })
        collectJob.cancel()
    }

    @Test
    fun `cancel delete clears pending delete`() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onRequestDelete(sampleIncomes[0])
        assertEquals(sampleIncomes[0], viewModel.pendingDeleteIncome.value)

        viewModel.cancelDelete()
        assertNull(viewModel.pendingDeleteIncome.value)
        collectJob.cancel()
    }

    @Test
    fun `empty repository emits Empty state`() = runTest(testDispatcher) {
        val emptyRepo = FakeIncomeRepository(mutableListOf())
        val emptyViewModel = IncomeListViewModel(
            getIncomesUseCase = GetIncomesUseCase(emptyRepo),
            deleteIncomeUseCase = DeleteIncomeUseCase(emptyRepo),
            zoneId = zoneId,
            clock = fixedClock
        )
        val collectJob = launch { emptyViewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = emptyViewModel.uiState.value
        assertTrue(state is IncomeListUiState.Empty)
        assertEquals(false, (state as IncomeListUiState.Empty).isFiltered)
        collectJob.cancel()
    }

    private class FakeIncomeRepository(
        private val list: MutableList<Income>
    ) : IncomeRepository {
        private val flow = MutableStateFlow(list.toList())

        override fun getAllIncomes(): Flow<List<Income>> = flow

        override suspend fun getIncomeById(id: Long): Income? = list.find { it.id == id }

        override suspend fun insertIncome(income: Income): Long {
            val id = (list.maxOfOrNull { it.id } ?: 0L) + 1L
            val created = income.copy(id = id)
            list.add(created)
            flow.value = list.toList()
            return id
        }

        override suspend fun updateIncome(income: Income) {
            val index = list.indexOfFirst { it.id == income.id }
            if (index != -1) {
                list[index] = income
                flow.value = list.toList()
            }
        }

        override suspend fun deleteIncome(income: Income) {
            list.removeAll { it.id == income.id }
            flow.value = list.toList()
        }

        override suspend fun deleteIncomeById(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }

        override fun getIncomesBySource(source: String): Flow<List<Income>> {
            return MutableStateFlow(list.filter { it.source.equals(source, ignoreCase = true) })
        }

        override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> {
            return MutableStateFlow(list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
        }

        override fun getTotalIncomeInCents(): Flow<Long?> {
            val total = list.sumOf { (it.amount * BigDecimal(100)).toLong() }
            return MutableStateFlow(total)
        }
    }
}

