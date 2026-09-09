package com.personalexpensetracker.ui.screens.expenses

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.usecase.UpdateExpenseUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class EditExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeExpenseRepository
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var viewModel: EditExpenseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeExpenseRepository()
        updateExpenseUseCase = UpdateExpenseUseCase(fakeRepository)
        viewModel = EditExpenseViewModel(updateExpenseUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setExpense_populatesUiStateCorrectly() {
        val original = Expense(
            id = 42L,
            title = "Groceries",
            amount = BigDecimal("85.50"),
            category = "Shopping",
            date = Instant.now()
        )

        viewModel.setExpense(original)

        val state = viewModel.uiState.value
        assertEquals(42L, state.id)
        assertEquals("Groceries", state.description)
        assertEquals("85.50", state.amount)
        assertEquals("Shopping", state.category)
        assertNull(state.errorMessage)
    }

    @Test
    fun onSaveChangesClick_withEmptyDescription_setsErrorMessage() {
        val original = Expense(
            id = 1L,
            title = "Lunch",
            amount = BigDecimal("15.00"),
            category = "Food",
            date = Instant.now()
        )
        viewModel.setExpense(original)
        viewModel.onDescriptionChange("   ")

        var successCalled = false
        viewModel.onSaveChangesClick { successCalled = true }

        val state = viewModel.uiState.value
        assertEquals("Description cannot be empty", state.errorMessage)
        assertEquals(false, successCalled)
    }

    @Test
    fun onSaveChangesClick_withInvalidAmount_setsErrorMessage() {
        val original = Expense(
            id = 1L,
            title = "Gas",
            amount = BigDecimal("40.00"),
            category = "Transportation",
            date = Instant.now()
        )
        viewModel.setExpense(original)
        viewModel.onAmountChange("0")

        var successCalled = false
        viewModel.onSaveChangesClick { successCalled = true }

        val state = viewModel.uiState.value
        assertEquals("Amount must be a valid number greater than 0", state.errorMessage)
        assertEquals(false, successCalled)
    }

    @Test
    fun onSaveChangesClick_withValidChanges_updatesExistingRecordWithoutDuplicates() = runTest(testDispatcher) {
        val initialExpense = Expense(
            id = 10L,
            title = "Old Title",
            amount = BigDecimal("20.00"),
            category = "Food",
            date = Instant.now()
        )
        fakeRepository.expenses.add(initialExpense)

        viewModel.setExpense(initialExpense)
        viewModel.onDescriptionChange("Updated Title")
        viewModel.onAmountChange("29.99")
        viewModel.onCategoryChange("Entertainment")
        viewModel.onDateChange(LocalDate.of(2026, 9, 7))

        var successCalled = false
        viewModel.onSaveChangesClick { successCalled = true }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(1, fakeRepository.expenses.size) // No duplicates
        val updated = fakeRepository.expenses[0]
        assertEquals(10L, updated.id) // Preserves ID
        assertEquals("Updated Title", updated.title)
        assertEquals("29.99", updated.amount.toPlainString())
        assertEquals("Entertainment", updated.category)
    }

    private class FakeExpenseRepository : ExpenseRepository {
        val expenses = mutableListOf<Expense>()

        override fun getAllExpenses(): Flow<List<Expense>> = flowOf(expenses)
        override suspend fun getExpenseById(id: Long): Expense? = expenses.find { it.id == id }
        override suspend fun insertExpense(expense: Expense): Long {
            expenses.add(expense)
            return expense.id
        }
        override suspend fun updateExpense(expense: Expense) {
            val index = expenses.indexOfFirst { it.id == expense.id }
            if (index != -1) {
                expenses[index] = expense
            }
        }
        override suspend fun deleteExpense(expense: Expense) {}
        override suspend fun deleteExpenseById(id: Long) {}
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> = flowOf(emptyList())
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> = flowOf(emptyList())
        override fun getTotalExpensesInCents(): Flow<Long?> = flowOf(0L)
    }
}

