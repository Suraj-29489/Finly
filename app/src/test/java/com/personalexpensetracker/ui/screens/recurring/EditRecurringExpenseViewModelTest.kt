package com.personalexpensetracker.ui.screens.recurring

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import com.personalexpensetracker.domain.usecase.GetRecurringExpenseByIdUseCase
import com.personalexpensetracker.domain.usecase.UpdateRecurringExpenseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EditRecurringExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRecurringExpenseRepository
    private lateinit var getByIdUseCase: GetRecurringExpenseByIdUseCase
    private lateinit var updateUseCase: UpdateRecurringExpenseUseCase
    private lateinit var viewModel: EditRecurringExpenseViewModel

    private val sampleRecurring = RecurringExpense(
        id = 42L,
        title = "Spotify",
        amount = BigDecimal("9.99"),
        category = "Entertainment",
        frequency = RecurrenceFrequency.MONTHLY,
        startDate = LocalDate.of(2026, 8, 15),
        nextOccurrenceDate = LocalDate.of(2026, 9, 15),
        isActive = true,
        endDate = null,
        notes = "Family plan"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecurringExpenseRepository()
        fakeRepository.items[42L] = sampleRecurring
        getByIdUseCase = GetRecurringExpenseByIdUseCase(fakeRepository)
        updateUseCase = UpdateRecurringExpenseUseCase(fakeRepository)
        viewModel = EditRecurringExpenseViewModel(getByIdUseCase, updateUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setRecurringExpense_populatesUiStateCorrectly() {
        viewModel.setRecurringExpense(sampleRecurring)

        val state = viewModel.uiState.value
        assertEquals(42L, state.id)
        assertEquals("Spotify", state.title)
        assertEquals("9.99", state.amount)
        assertEquals("Entertainment", state.category)
        assertEquals(RecurrenceFrequency.MONTHLY, state.frequency)
        assertEquals(LocalDate.of(2026, 8, 15), state.startDate)
        assertEquals(LocalDate.of(2026, 9, 15), state.nextOccurrenceDate)
        assertTrue(state.isActive)
        assertNull(state.endDate)
        assertFalse(state.hasEndDate)
        assertEquals("Family plan", state.notes)
    }

    @Test
    fun loadRecurringExpense_retrievesAndPopulatesState() = runTest(testDispatcher) {
        viewModel.loadRecurringExpense(42L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(42L, state.id)
        assertEquals("Spotify", state.title)
        assertEquals("9.99", state.amount)
    }

    @Test
    fun loadRecurringExpense_notFound_showsError() = runTest(testDispatcher) {
        viewModel.loadRecurringExpense(999L)
        advanceUntilIdle()

        assertEquals("Recurring expense not found", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onIsActiveChange_updatesActiveState() {
        viewModel.setRecurringExpense(sampleRecurring)
        viewModel.onIsActiveChange(false)
        assertFalse(viewModel.uiState.value.isActive)

        viewModel.onIsActiveChange(true)
        assertTrue(viewModel.uiState.value.isActive)
    }

    @Test
    fun onSaveChangesClick_validChanges_persistsAndCallsCallback() = runTest(testDispatcher) {
        viewModel.setRecurringExpense(sampleRecurring)
        viewModel.onTitleChange("Spotify Premium Duo")
        viewModel.onAmountChange("14.99")
        viewModel.onFrequencyChange(RecurrenceFrequency.YEARLY)
        viewModel.onIsActiveChange(false)

        var callbackInvoked = false
        viewModel.onSaveChangesClick(onSuccess = { callbackInvoked = true })
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        val updated = fakeRepository.items[42L]!!
        assertEquals("Spotify Premium Duo", updated.title)
        assertEquals(BigDecimal("14.99"), updated.amount)
        assertEquals(RecurrenceFrequency.YEARLY, updated.frequency)
        assertFalse(updated.isActive)
    }

    @Test
    fun onSaveChangesClick_blankTitle_showsError() = runTest(testDispatcher) {
        viewModel.setRecurringExpense(sampleRecurring)
        viewModel.onTitleChange("   ")

        viewModel.onSaveChangesClick(onSuccess = {})
        advanceUntilIdle()

        assertEquals("Title cannot be blank", viewModel.uiState.value.errorMessage)
        assertEquals("Spotify", fakeRepository.items[42L]!!.title)
    }

    @Test
    fun onSaveChangesClick_invalidAmount_showsError() = runTest(testDispatcher) {
        viewModel.setRecurringExpense(sampleRecurring)
        viewModel.onAmountChange("0.00")

        viewModel.onSaveChangesClick(onSuccess = {})
        advanceUntilIdle()

        assertEquals("Amount must be greater than zero", viewModel.uiState.value.errorMessage)
        assertEquals(BigDecimal("9.99"), fakeRepository.items[42L]!!.amount)
    }

    @Test
    fun onSaveChangesClick_invalidEndDate_showsError() = runTest(testDispatcher) {
        viewModel.setRecurringExpense(sampleRecurring)
        viewModel.onHasEndDateToggle(true)
        viewModel.onEndDateChange(LocalDate.of(2026, 8, 1)) // Before start date (Aug 15)

        viewModel.onSaveChangesClick(onSuccess = {})
        advanceUntilIdle()

        assertEquals("End date cannot be before start date", viewModel.uiState.value.errorMessage)
    }

    private class FakeRecurringExpenseRepository : RecurringExpenseRepository {
        val items = mutableMapOf<Long, RecurringExpense>()
        private var nextId = 1L

        override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> = flowOf(items.values.toList())
        override fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> = flowOf(items.values.filter { it.isActive })
        override fun getRecurringExpensesByCategory(category: String): Flow<List<RecurringExpense>> = flowOf(items.values.filter { it.category == category })
        override suspend fun getRecurringExpenseById(id: Long): RecurringExpense? = items[id]
        override suspend fun getDueRecurringExpenses(date: LocalDate): List<RecurringExpense> = items.values.filter { it.isActive && !it.nextOccurrenceDate.isAfter(date) }

        override suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long {
            val id = nextId++
            items[id] = recurringExpense.copy(id = id)
            return id
        }

        override suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) {
            items[recurringExpense.id] = recurringExpense
        }

        override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
            items.remove(recurringExpense.id)
        }

        override suspend fun deleteRecurringExpenseById(id: Long) {
            items.remove(id)
        }

        override suspend fun setActive(id: Long, isActive: Boolean) {
            items[id]?.let { items[id] = it.copy(isActive = isActive) }
        }

        override suspend fun updateNextOccurrence(id: Long, nextOccurrenceDate: LocalDate, lastGeneratedDate: LocalDate) {
            items[id]?.let { items[id] = it.copy(nextOccurrenceDate = nextOccurrenceDate, lastGeneratedDate = lastGeneratedDate) }
        }
    }
}

