package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.validation.ExpenseValidator
import com.personalexpensetracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Unit tests for domain use cases and validation logic in Phase 2 Step 1.
 */
class ExpenseUseCasesTest {

    private lateinit var fakeRepository: FakeExpenseRepository
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var getExpensesUseCase: GetExpensesUseCase
    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase

    @Before
    fun setup() {
        fakeRepository = FakeExpenseRepository()
        addExpenseUseCase = AddExpenseUseCase(fakeRepository)
        getExpensesUseCase = GetExpensesUseCase(fakeRepository)
        getExpenseByIdUseCase = GetExpenseByIdUseCase(fakeRepository)
        updateExpenseUseCase = UpdateExpenseUseCase(fakeRepository)
        deleteExpenseUseCase = DeleteExpenseUseCase(fakeRepository)
    }

    @Test
    fun addExpenseUseCase_validExpense_insertsSuccessfully() = runBlocking {
        val expense = Expense(
            title = "Coffee",
            amount = BigDecimal("4.50"),
            category = "Food",
            date = Instant.now()
        )

        val id = addExpenseUseCase(expense)
        assertEquals(1L, id)
        assertEquals(1, fakeRepository.expenses.size)
        assertEquals("Coffee", fakeRepository.expenses[0].title)
    }

    @Test
    fun addExpenseUseCase_blankTitle_throwsException() {
        val expense = Expense(
            title = "   ",
            amount = BigDecimal("10.00"),
            category = "Food",
            date = Instant.now()
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addExpenseUseCase(expense) }
        }
        assertEquals("Title cannot be blank", exception.message)
    }

    @Test
    fun addExpenseUseCase_zeroAmount_throwsException() {
        val expense = Expense(
            title = "Free Snack",
            amount = BigDecimal.ZERO,
            category = "Food",
            date = Instant.now()
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addExpenseUseCase(expense) }
        }
        assertEquals("Amount must be greater than zero", exception.message)
    }

    @Test
    fun addExpenseUseCase_negativeAmount_throwsException() {
        val expense = Expense(
            title = "Refund",
            amount = BigDecimal("-5.00"),
            category = "Food",
            date = Instant.now()
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addExpenseUseCase(expense) }
        }
        assertEquals("Amount must be greater than zero", exception.message)
    }

    @Test
    fun addExpenseUseCase_blankCategory_throwsException() {
        val expense = Expense(
            title = "Lunch",
            amount = BigDecimal("12.00"),
            category = "  ",
            date = Instant.now()
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addExpenseUseCase(expense) }
        }
        assertEquals("Category cannot be blank", exception.message)
    }

    @Test
    fun getExpensesUseCase_returnsFlowFromRepository() = runBlocking {
        fakeRepository.insertExpense(Expense(title = "Item 1", amount = BigDecimal("10.00"), category = "General", date = Instant.now()))
        fakeRepository.insertExpense(Expense(title = "Item 2", amount = BigDecimal("20.00"), category = "General", date = Instant.now()))

        val list = getExpensesUseCase().first()
        assertEquals(2, list.size)
    }

    @Test
    fun getExpenseByIdUseCase_returnsMatchingExpense() = runBlocking {
        val id = fakeRepository.insertExpense(Expense(title = "Movie", amount = BigDecimal("15.00"), category = "Entertainment", date = Instant.now()))

        val retrieved = getExpenseByIdUseCase(id)
        assertNotNull(retrieved)
        assertEquals("Movie", retrieved?.title)

        val notFound = getExpenseByIdUseCase(999L)
        assertNull(notFound)
    }

    @Test
    fun updateExpenseUseCase_validExpense_updatesRepository() = runBlocking {
        val id = fakeRepository.insertExpense(Expense(title = "Bus", amount = BigDecimal("2.50"), category = "Transport", date = Instant.now()))
        val original = fakeRepository.getExpenseById(id)!!

        val updated = original.copy(title = "Metro", amount = BigDecimal("3.00"))
        updateExpenseUseCase(updated)

        val retrieved = fakeRepository.getExpenseById(id)
        assertEquals("Metro", retrieved?.title)
        assertEquals(BigDecimal("3.00"), retrieved?.amount)
    }

    @Test
    fun updateExpenseUseCase_invalidExpense_throwsException() {
        val expense = Expense(id = 1L, title = "", amount = BigDecimal("5.00"), category = "Food", date = Instant.now())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { updateExpenseUseCase(expense) }
        }
        assertEquals("Title cannot be blank", exception.message)
    }

    @Test
    fun deleteExpenseUseCase_deletesByEntityAndById() = runBlocking {
        val id1 = fakeRepository.insertExpense(Expense(title = "A", amount = BigDecimal("1.00"), category = "Misc", date = Instant.now()))
        val id2 = fakeRepository.insertExpense(Expense(title = "B", amount = BigDecimal("2.00"), category = "Misc", date = Instant.now()))

        assertEquals(2, fakeRepository.expenses.size)

        // Delete by entity
        val exp1 = fakeRepository.getExpenseById(id1)!!
        deleteExpenseUseCase(exp1)
        assertEquals(1, fakeRepository.expenses.size)
        assertNull(fakeRepository.getExpenseById(id1))

        // Delete by ID
        deleteExpenseUseCase(id2)
        assertTrue(fakeRepository.expenses.isEmpty())
        assertNull(fakeRepository.getExpenseById(id2))
    }

    @Test
    fun expenseValidator_returnsExpectedValidationResults() {
        assertEquals(ValidationResult.Valid, ExpenseValidator.validate("Groceries", BigDecimal("25.00"), "Food"))
        assertTrue(ExpenseValidator.validate("", BigDecimal("25.00"), "Food") is ValidationResult.Invalid)
        assertTrue(ExpenseValidator.validate("Gas", BigDecimal.ZERO, "Transport") is ValidationResult.Invalid)
        assertTrue(ExpenseValidator.validate("Gas", BigDecimal("-1.00"), "Transport") is ValidationResult.Invalid)
        assertTrue(ExpenseValidator.validate("Gas", BigDecimal("10.00"), "") is ValidationResult.Invalid)
    }

    /**
     * In-memory fake repository implementation for fast, isolated unit testing of use cases.
     */
    private class FakeExpenseRepository : ExpenseRepository {
        val expenses = mutableListOf<Expense>()
        private var nextId = 1L

        override fun getAllExpenses(): Flow<List<Expense>> {
            return flowOf(expenses.sortedByDescending { it.date })
        }

        override suspend fun getExpenseById(id: Long): Expense? {
            return expenses.find { it.id == id }
        }

        override suspend fun insertExpense(expense: Expense): Long {
            val assignedId = if (expense.id == 0L) nextId++ else expense.id
            val stored = expense.copy(id = assignedId)
            expenses.removeIf { it.id == assignedId }
            expenses.add(stored)
            return assignedId
        }

        override suspend fun updateExpense(expense: Expense) {
            val index = expenses.indexOfFirst { it.id == expense.id }
            if (index != -1) {
                expenses[index] = expense
            }
        }

        override suspend fun deleteExpense(expense: Expense) {
            expenses.removeIf { it.id == expense.id }
        }

        override suspend fun deleteExpenseById(id: Long) {
            expenses.removeIf { it.id == id }
        }

        override fun getExpensesByCategory(category: String): Flow<List<Expense>> {
            return flowOf(expenses.filter { it.category == category })
        }

        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> {
            return flowOf(expenses.filter { it.date in startDate..endDate })
        }

        override fun getTotalExpensesInCents(): Flow<Long?> {
            val total = expenses.sumOf { it.amount.movePointRight(2).toLong() }
            return flowOf(total)
        }
    }
}

