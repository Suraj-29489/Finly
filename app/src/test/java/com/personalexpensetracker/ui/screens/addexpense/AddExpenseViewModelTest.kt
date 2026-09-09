package com.personalexpensetracker.ui.screens.addexpense

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.usecase.AddExpenseUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeExpenseRepository
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var viewModel: AddExpenseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeExpenseRepository()
        addExpenseUseCase = AddExpenseUseCase(fakeRepository)
        viewModel = AddExpenseViewModel(addExpenseUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onAmountChange_filtersInvalidCharacters() {
        viewModel.onAmountChange("12.3.4")
        assertEquals("", viewModel.uiState.value.amount)

        viewModel.onAmountChange("45.50")
        assertEquals("45.50", viewModel.uiState.value.amount)

        viewModel.onAmountChange("abc")
        assertEquals("", viewModel.uiState.value.amount)
    }

    @Test
    fun onAddExpenseClick_withEmptyDescription_setsErrorMessage() {
        viewModel.onAmountChange("25.00")
        viewModel.onDescriptionChange("   ")
        viewModel.onAddExpenseClick()

        val state = viewModel.uiState.value
        assertEquals("Description cannot be empty", state.errorMessage)
        assertNull(state.successMessage)
        assertEquals(0, fakeRepository.expenses.size)
    }

    @Test
    fun onAddExpenseClick_withZeroAmount_setsErrorMessage() {
        viewModel.onDescriptionChange("Coffee")
        viewModel.onAmountChange("0")
        viewModel.onAddExpenseClick()

        val state = viewModel.uiState.value
        assertEquals("Amount must be a valid number greater than 0", state.errorMessage)
        assertNull(state.successMessage)
        assertEquals(0, fakeRepository.expenses.size)
    }

    @Test
    fun onAddExpenseClick_withValidData_resetsFormAndSetsSuccessMessage() = runTest(testDispatcher) {
        viewModel.onDescriptionChange("Lunch")
        viewModel.onAmountChange("15.75")
        viewModel.onCategoryChange("Food")
        viewModel.onDateChange(LocalDate.now())

        viewModel.onAddExpenseClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.amount)
        assertEquals("", state.description)
        assertNull(state.errorMessage)
        assertEquals("Expense added successfully!", state.successMessage)

        assertEquals(1, fakeRepository.expenses.size)
        val saved = fakeRepository.expenses[0]
        assertEquals("Lunch", saved.title)
        assertEquals("15.75", saved.amount.toPlainString())
        assertEquals("Food", saved.category)
    }

    private class FakeExpenseRepository : ExpenseRepository {
        val expenses = mutableListOf<Expense>()
        private var nextId = 1L

        override fun getAllExpenses(): Flow<List<Expense>> = flowOf(expenses)
        override suspend fun getExpenseById(id: Long): Expense? = expenses.find { it.id == id }
        override suspend fun insertExpense(expense: Expense): Long {
            val assignedId = nextId++
            expenses.add(expense.copy(id = assignedId))
            return assignedId
        }
        override suspend fun updateExpense(expense: Expense) {}
        override suspend fun deleteExpense(expense: Expense) {}
        override suspend fun deleteExpenseById(id: Long) {}
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> = flowOf(emptyList())
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> = flowOf(emptyList())
        override fun getTotalExpensesInCents(): Flow<Long?> = flowOf(0L)
    }
}

