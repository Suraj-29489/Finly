package com.personalexpensetracker.ui.screens.income

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository
import com.personalexpensetracker.domain.usecase.GetIncomeByIdUseCase
import com.personalexpensetracker.domain.usecase.UpdateIncomeUseCase
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
class EditIncomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeIncomeRepository
    private lateinit var getIncomeByIdUseCase: GetIncomeByIdUseCase
    private lateinit var updateIncomeUseCase: UpdateIncomeUseCase
    private lateinit var viewModel: EditIncomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeIncomeRepository()
        getIncomeByIdUseCase = GetIncomeByIdUseCase(fakeRepository)
        updateIncomeUseCase = UpdateIncomeUseCase(fakeRepository)
        viewModel = EditIncomeViewModel(getIncomeByIdUseCase, updateIncomeUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setIncome_populatesUiStateCorrectly() {
        val income = Income(
            id = 5L,
            title = "Stock Dividend",
            amount = BigDecimal("150.25"),
            source = "Investment",
            date = Instant.now(),
            notes = "Q3 Payout"
        )

        viewModel.setIncome(income)

        val state = viewModel.uiState.value
        assertEquals(5L, state.id)
        assertEquals("Stock Dividend", state.title)
        assertEquals("150.25", state.amount)
        assertEquals("Investment", state.source)
        assertEquals("Q3 Payout", state.notes)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadIncome_retrievesAndPopulatesState() = runTest(testDispatcher) {
        val income = Income(
            id = 10L,
            title = "Freelance UI Design",
            amount = BigDecimal("850.00"),
            source = "Freelance",
            date = Instant.now(),
            notes = "Client Alpha"
        )
        fakeRepository.incomes.add(income)

        viewModel.loadIncome(10L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(10L, state.id)
        assertEquals("Freelance UI Design", state.title)
        assertEquals("850.00", state.amount)
        assertEquals("Freelance", state.source)
    }

    @Test
    fun loadIncome_notFound_setsErrorMessage() = runTest(testDispatcher) {
        viewModel.loadIncome(999L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Income transaction not found", state.errorMessage)
    }

    @Test
    fun onSaveChangesClick_validChanges_updatesAndCallsSuccess() = runTest(testDispatcher) {
        val income = Income(
            id = 1L,
            title = "Old Title",
            amount = BigDecimal("100.00"),
            source = "Other",
            date = Instant.now()
        )
        fakeRepository.incomes.add(income)
        viewModel.setIncome(income)

        viewModel.onTitleChange("New Consulting Title")
        viewModel.onAmountChange("250.00")
        viewModel.onSourceChange("business")
        viewModel.onNotesChange("Updated notes")

        var successCalled = false
        viewModel.onSaveChangesClick {
            successCalled = true
        }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(viewModel.uiState.value.isUpdated)

        val persisted = fakeRepository.incomes.find { it.id == 1L }
        assertNotNull(persisted)
        assertEquals("New Consulting Title", persisted?.title)
        assertEquals("250.00", persisted?.amount?.toPlainString())
        assertEquals("Business", persisted?.source)
        assertEquals("Updated notes", persisted?.notes)
    }

    @Test
    fun onSaveChangesClick_blankTitle_showsError() {
        val income = Income(
            id = 1L,
            title = "Valid Title",
            amount = BigDecimal("100.00"),
            source = "Salary",
            date = Instant.now()
        )
        viewModel.setIncome(income)
        viewModel.onTitleChange("   ")

        var successCalled = false
        viewModel.onSaveChangesClick { successCalled = true }

        assertTrue(!successCalled)
        assertEquals("Title cannot be blank", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSaveChangesClick_invalidAmount_showsError() {
        val income = Income(
            id = 1L,
            title = "Valid Title",
            amount = BigDecimal("100.00"),
            source = "Salary",
            date = Instant.now()
        )
        viewModel.setIncome(income)
        viewModel.onAmountChange("0")

        var successCalled = false
        viewModel.onSaveChangesClick { successCalled = true }

        assertTrue(!successCalled)
        assertEquals("Amount must be greater than zero", viewModel.uiState.value.errorMessage)
    }

    private class FakeIncomeRepository : IncomeRepository {
        val incomes = mutableListOf<Income>()

        override fun getAllIncomes(): Flow<List<Income>> = flowOf(incomes)
        override suspend fun getIncomeById(id: Long): Income? = incomes.find { it.id == id }
        override suspend fun insertIncome(income: Income): Long {
            incomes.add(income)
            return income.id
        }
        override suspend fun updateIncome(income: Income) {
            val index = incomes.indexOfFirst { it.id == income.id }
            if (index != -1) {
                incomes[index] = income
            }
        }
        override suspend fun deleteIncome(income: Income) {}
        override suspend fun deleteIncomeById(id: Long) {}
        override fun getIncomesBySource(source: String): Flow<List<Income>> = flowOf(emptyList())
        override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> = flowOf(emptyList())
        override fun getTotalIncomeInCents(): Flow<Long?> = flowOf(0L)
    }
}

