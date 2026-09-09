package com.personalexpensetracker.ui.screens.income

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.repository.IncomeRepository
import com.personalexpensetracker.domain.usecase.AddIncomeUseCase
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddIncomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeIncomeRepository
    private lateinit var addIncomeUseCase: AddIncomeUseCase
    private lateinit var viewModel: AddIncomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeIncomeRepository()
        addIncomeUseCase = AddIncomeUseCase(fakeRepository)
        viewModel = AddIncomeViewModel(addIncomeUseCase)
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
        assertEquals(IncomeSource.DEFAULT, state.source)
        assertEquals(LocalDate.now(), state.date)
        assertEquals("", state.notes)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun onAmountChange_filtersInvalidCharacters() {
        viewModel.onAmountChange("12.3.4")
        assertEquals("", viewModel.uiState.value.amount)

        viewModel.onAmountChange("5000.00")
        assertEquals("5000.00", viewModel.uiState.value.amount)

        viewModel.onAmountChange("xyz")
        assertEquals("", viewModel.uiState.value.amount)
    }

    @Test
    fun onAddIncomeClick_withEmptyTitle_setsErrorMessage() {
        viewModel.onAmountChange("1000.00")
        viewModel.onTitleChange("   ")
        viewModel.onAddIncomeClick()

        val state = viewModel.uiState.value
        assertEquals("Title cannot be blank", state.errorMessage)
        assertNull(state.successMessage)
        assertEquals(0, fakeRepository.incomes.size)
    }

    @Test
    fun onAddIncomeClick_withZeroOrNegativeAmount_setsErrorMessage() {
        viewModel.onTitleChange("Salary")
        viewModel.onAmountChange("0")
        viewModel.onAddIncomeClick()

        val state1 = viewModel.uiState.value
        assertEquals("Amount must be greater than zero", state1.errorMessage)
        assertEquals(0, fakeRepository.incomes.size)

        viewModel.onAmountChange("0.00")
        viewModel.onAddIncomeClick()
        val state2 = viewModel.uiState.value
        assertEquals("Amount must be greater than zero", state2.errorMessage)
    }

    @Test
    fun onAddIncomeClick_withExcessiveAmount_setsErrorMessage() {
        viewModel.onTitleChange("Sale")
        viewModel.onAmountChange("100000000.01")
        viewModel.onAddIncomeClick()

        val state = viewModel.uiState.value
        assertEquals("Amount cannot exceed 100,000,000", state.errorMessage)
        assertEquals(0, fakeRepository.incomes.size)
    }

    @Test
    fun onAddIncomeClick_withEmptySource_setsErrorMessage() {
        viewModel.onTitleChange("Consulting")
        viewModel.onAmountChange("200.00")
        viewModel.onSourceChange("   ")
        viewModel.onAddIncomeClick()

        val state = viewModel.uiState.value
        assertEquals("Income source cannot be blank", state.errorMessage)
        assertEquals(0, fakeRepository.incomes.size)
    }

    @Test
    fun onAddIncomeClick_withValidData_resetsFormAndSetsSuccessMessage() = runTest(testDispatcher) {
        viewModel.onTitleChange("Monthly Salary")
        viewModel.onAmountChange("4500.00")
        viewModel.onSourceChange("salary")
        viewModel.onNotesChange("Bi-weekly paycheck")
        viewModel.onDateChange(LocalDate.of(2026, 9, 7))

        viewModel.onAddIncomeClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.amount)
        assertEquals("", state.notes)
        assertNull(state.errorMessage)
        assertEquals("Income \"Monthly Salary\" recorded successfully!", state.successMessage)
        assertNotNull(state.lastInsertedIncome)

        assertEquals(1, fakeRepository.incomes.size)
        val saved = fakeRepository.incomes[0]
        assertEquals("Monthly Salary", saved.title)
        assertEquals("4500.00", saved.amount.toPlainString())
        assertEquals("Salary", saved.source)
        assertEquals("Bi-weekly paycheck", saved.notes)
    }

    @Test
    fun clearErrorAndSuccessMessages() {
        viewModel.onAddIncomeClick() // triggers empty title error
        assertEquals("Title cannot be blank", viewModel.uiState.value.errorMessage)
        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private class FakeIncomeRepository : IncomeRepository {
        val incomes = mutableListOf<Income>()
        private var nextId = 1L

        override fun getAllIncomes(): Flow<List<Income>> = flowOf(incomes)
        override suspend fun getIncomeById(id: Long): Income? = incomes.find { it.id == id }
        override suspend fun insertIncome(income: Income): Long {
            val assignedId = nextId++
            incomes.add(income.copy(id = assignedId))
            return assignedId
        }
        override suspend fun updateIncome(income: Income) {}
        override suspend fun deleteIncome(income: Income) {}
        override suspend fun deleteIncomeById(id: Long) {}
        override fun getIncomesBySource(source: String): Flow<List<Income>> = flowOf(emptyList())
        override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> = flowOf(emptyList())
        override fun getTotalIncomeInCents(): Flow<Long?> = flowOf(0L)
    }
}
