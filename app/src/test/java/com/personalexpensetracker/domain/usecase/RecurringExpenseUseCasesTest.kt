package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurringExpenseUseCasesTest {

    private lateinit var fakeRepository: FakeRecurringExpenseRepository
    private val startDate = LocalDate.of(2026, 9, 1)

    @Before
    fun setUp() {
        fakeRepository = FakeRecurringExpenseRepository()
    }

    @Test
    fun createRecurringExpenseUseCaseNormalizesCategoryAndSucceeds() = runBlocking {
        val useCase = CreateRecurringExpenseUseCase(fakeRepository)
        val recurring = RecurringExpense(
            title = "Gym",
            amount = BigDecimal("45.00"),
            category = "health ",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            nextOccurrenceDate = startDate
        )

        val id = useCase(recurring)
        assertTrue(id > 0)
        val saved = fakeRepository.items[id]
        assertNotNull(saved)
        assertEquals("Health", saved?.category) // Normalized to canonical Health
    }

    @Test
    fun createRecurringExpenseUseCaseThrowsForInvalidAmount() {
        val useCase = CreateRecurringExpenseUseCase(fakeRepository)
        val recurring = RecurringExpense(
            title = "Gym",
            amount = BigDecimal("-10.00"),
            category = "Health",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            nextOccurrenceDate = startDate
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(recurring) }
        }
    }

    @Test
    fun updateRecurringExpenseUseCaseNormalizesCategoryAndUpdates() = runBlocking {
        val recurring = RecurringExpense(
            id = 1L,
            title = "Gym",
            amount = BigDecimal("45.00"),
            category = "Health",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            nextOccurrenceDate = startDate
        )
        fakeRepository.items[1L] = recurring

        val useCase = UpdateRecurringExpenseUseCase(fakeRepository)
        val updated = recurring.copy(amount = BigDecimal("50.00"), category = "food ")
        useCase(updated)

        val saved = fakeRepository.items[1L]
        assertEquals(BigDecimal("50.00"), saved?.amount)
        assertEquals("Food", saved?.category)
    }

    @Test
    fun toggleRecurringExpenseActiveUseCaseChangesState() = runBlocking {
        val recurring = RecurringExpense(
            id = 1L,
            title = "Netflix",
            amount = BigDecimal("15.99"),
            category = "Entertainment",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            nextOccurrenceDate = startDate,
            isActive = true
        )
        fakeRepository.items[1L] = recurring

        val useCase = ToggleRecurringExpenseActiveUseCase(fakeRepository)
        useCase(1L, false)
        assertFalse(fakeRepository.items[1L]!!.isActive)

        useCase(1L, true)
        assertTrue(fakeRepository.items[1L]!!.isActive)
    }

    @Test
    fun deleteRecurringExpenseUseCaseRemovesItem() = runBlocking {
        val recurring = RecurringExpense(
            id = 1L,
            title = "Old Sub",
            amount = BigDecimal("5.00"),
            category = "Other",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            nextOccurrenceDate = startDate
        )
        fakeRepository.items[1L] = recurring

        val useCase = DeleteRecurringExpenseUseCase(fakeRepository)
        useCase(1L)
        assertFalse(fakeRepository.items.containsKey(1L))
    }

    @Test
    fun getAllAndGetActiveRecurringExpensesUseCasesEmitCorrectLists() = runBlocking {
        fakeRepository.items[1L] = RecurringExpense(
            id = 1L, title = "Active", amount = BigDecimal("10.00"), category = "Bills",
            frequency = RecurrenceFrequency.MONTHLY, startDate = startDate, nextOccurrenceDate = startDate, isActive = true
        )
        fakeRepository.items[2L] = RecurringExpense(
            id = 2L, title = "Paused", amount = BigDecimal("20.00"), category = "Bills",
            frequency = RecurrenceFrequency.MONTHLY, startDate = startDate, nextOccurrenceDate = startDate, isActive = false
        )

        val allUseCase = GetAllRecurringExpensesUseCase(fakeRepository)
        assertEquals(2, allUseCase().first().size)

        val activeUseCase = GetActiveRecurringExpensesUseCase(fakeRepository)
        val activeList = activeUseCase().first()
        assertEquals(1, activeList.size)
        assertEquals("Active", activeList[0].title)
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

