package com.personalexpensetracker.ui.screens.recurring

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import com.personalexpensetracker.domain.usecase.CreateRecurringExpenseUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AddRecurringExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneOffset.UTC
    private val fixedNow = Instant.parse("2026-09-07T12:00:00Z")
    private val fixedClock = Clock.fixed(fixedNow, zoneId)
    private lateinit var fakeRepository: FakeRecurringExpenseRepository
    private lateinit var createUseCase: CreateRecurringExpenseUseCase
    private lateinit var viewModel: AddRecurringExpenseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRecurringExpenseRepository()
        createUseCase = CreateRecurringExpenseUseCase(fakeRepository)
        viewModel = AddRecurringExpenseViewModel(createUseCase, zoneId, fixedClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultValues() {
        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.amount)
        assertEquals(Category.DEFAULT, state.category)
        assertEquals(RecurrenceFrequency.MONTHLY, state.frequency)
        assertEquals(LocalDate.of(2026, 9, 7), state.startDate)
        assertNull(state.endDate)
        assertFalse(state.hasEndDate)
        assertEquals("", state.notes)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun onFrequencyChange_updatesFrequency() {
        viewModel.onFrequencyChange(RecurrenceFrequency.YEARLY)
        assertEquals(RecurrenceFrequency.YEARLY, viewModel.uiState.value.frequency)

        viewModel.onFrequencyChange(RecurrenceFrequency.WEEKLY)
        assertEquals(RecurrenceFrequency.WEEKLY, viewModel.uiState.value.frequency)

        viewModel.onFrequencyChange(RecurrenceFrequency.DAILY)
        assertEquals(RecurrenceFrequency.DAILY, viewModel.uiState.value.frequency)
    }

    @Test
    fun onAmountChange_sanitizesInput() {
        viewModel.onAmountChange("12.34")
        assertEquals("12.34", viewModel.uiState.value.amount)

        viewModel.onAmountChange("12.34.56") // Extra dot ignored
        assertEquals("12.34", viewModel.uiState.value.amount)

        viewModel.onAmountChange("abc45.99xyz") // Alphabets stripped
        assertEquals("45.99", viewModel.uiState.value.amount)
    }

    @Test
    fun onHasEndDateToggle_enablesAndDisablesEndDate() {
        viewModel.onHasEndDateToggle(true)
        assertTrue(viewModel.uiState.value.hasEndDate)
        assertNotNull(viewModel.uiState.value.endDate)

        val customEnd = LocalDate.of(2026, 12, 31)
        viewModel.onEndDateChange(customEnd)
        assertEquals(customEnd, viewModel.uiState.value.endDate)

        viewModel.onHasEndDateToggle(false)
        assertFalse(viewModel.uiState.value.hasEndDate)
        assertNull(viewModel.uiState.value.endDate)
    }

    @Test
    fun onSaveRecurringExpense_missingTitle_showsError() = runTest(testDispatcher) {
        viewModel.onAmountChange("50.00")
        viewModel.onTitleChange("   ") // Blank
        viewModel.onSaveRecurringExpense()

        advanceUntilIdle()
        assertEquals("Title cannot be blank", viewModel.uiState.value.errorMessage)
        assertTrue(fakeRepository.items.isEmpty())
    }

    @Test
    fun onSaveRecurringExpense_invalidAmount_showsError() = runTest(testDispatcher) {
        viewModel.onTitleChange("Netflix")
        viewModel.onAmountChange("0.00")
        viewModel.onSaveRecurringExpense()

        advanceUntilIdle()
        assertEquals("Amount must be greater than zero", viewModel.uiState.value.errorMessage)
        assertTrue(fakeRepository.items.isEmpty())
    }

    @Test
    fun onSaveRecurringExpense_endDateBeforeStartDate_showsError() = runTest(testDispatcher) {
        viewModel.onTitleChange("Gym Membership")
        viewModel.onAmountChange("30.00")
        viewModel.onStartDateChange(LocalDate.of(2026, 9, 10))
        viewModel.onHasEndDateToggle(true)
        viewModel.onEndDateChange(LocalDate.of(2026, 9, 5)) // Before start date

        viewModel.onSaveRecurringExpense()
        advanceUntilIdle()

        assertEquals("End date cannot be before start date", viewModel.uiState.value.errorMessage)
        assertTrue(fakeRepository.items.isEmpty())
    }

    @Test
    fun onSaveRecurringExpense_validInput_persistsAndResets() = runTest(testDispatcher) {
        viewModel.onTitleChange("Apartment Rent")
        viewModel.onAmountChange("1200.00")
        viewModel.onCategoryChange("Bills")
        viewModel.onFrequencyChange(RecurrenceFrequency.MONTHLY)
        viewModel.onStartDateChange(LocalDate.of(2026, 10, 1))
        viewModel.onNotesChange("Pay by 1st of month")

        var callbackInvoked = false
        viewModel.onSaveRecurringExpense(onSuccess = { callbackInvoked = true })

        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals(1, fakeRepository.items.size)
        val saved = fakeRepository.items.values.first()
        assertEquals("Apartment Rent", saved.title)
        assertEquals(BigDecimal("1200.00"), saved.amount)
        assertEquals("Bills", saved.category)
        assertEquals(RecurrenceFrequency.MONTHLY, saved.frequency)
        assertEquals(LocalDate.of(2026, 10, 1), saved.startDate)
        assertEquals(LocalDate.of(2026, 10, 1), saved.nextOccurrenceDate)
        assertTrue(saved.isActive)
        assertNull(saved.endDate)
        assertEquals("Pay by 1st of month", saved.notes)

        // Verifies form is reset
        assertEquals("", viewModel.uiState.value.title)
        assertEquals("", viewModel.uiState.value.amount)
        assertEquals("Recurring expense added successfully", viewModel.uiState.value.successMessage)
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

