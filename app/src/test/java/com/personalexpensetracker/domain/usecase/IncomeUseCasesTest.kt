package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.repository.IncomeRepository
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
import java.time.temporal.ChronoUnit

/**
 * Unit tests for domain use cases for Income in Phase 8 Step 1.
 */
class IncomeUseCasesTest {

    private lateinit var fakeRepository: FakeIncomeRepository
    private lateinit var addIncomeUseCase: AddIncomeUseCase
    private lateinit var getIncomesUseCase: GetIncomesUseCase
    private lateinit var getIncomeByIdUseCase: GetIncomeByIdUseCase
    private lateinit var updateIncomeUseCase: UpdateIncomeUseCase
    private lateinit var deleteIncomeUseCase: DeleteIncomeUseCase
    private lateinit var getTotalIncomeUseCase: GetTotalIncomeUseCase

    @Before
    fun setup() {
        fakeRepository = FakeIncomeRepository()
        addIncomeUseCase = AddIncomeUseCase(fakeRepository)
        getIncomesUseCase = GetIncomesUseCase(fakeRepository)
        getIncomeByIdUseCase = GetIncomeByIdUseCase(fakeRepository)
        updateIncomeUseCase = UpdateIncomeUseCase(fakeRepository)
        deleteIncomeUseCase = DeleteIncomeUseCase(fakeRepository)
        getTotalIncomeUseCase = GetTotalIncomeUseCase(fakeRepository)
    }

    @Test
    fun addIncomeUseCase_validIncome_insertsSuccessfully() = runBlocking {
        val income = Income(
            title = "Paycheck",
            amount = BigDecimal("3200.00"),
            source = "salary",
            date = Instant.now()
        )

        val id = addIncomeUseCase(income)
        assertEquals(1L, id)
        assertEquals(1, fakeRepository.incomes.size)
        // Source should be normalized to canonical "Salary"
        assertEquals("Salary", fakeRepository.incomes[0].source)
    }

    @Test
    fun addIncomeUseCase_normalizesCustomOrBuiltInSource() = runBlocking {
        val incomeBuiltIn = Income(
            title = "Consulting",
            amount = BigDecimal("500.00"),
            source = "   freelance   ",
            date = Instant.now()
        )
        addIncomeUseCase(incomeBuiltIn)
        assertEquals("Freelance", fakeRepository.incomes[0].source)

        val incomeCustom = Income(
            title = "Rental Room",
            amount = BigDecimal("800.00"),
            source = "   Rental Property  ",
            date = Instant.now()
        )
        addIncomeUseCase(incomeCustom)
        assertEquals("Rental Property", fakeRepository.incomes[1].source)
    }

    @Test
    fun addIncomeUseCase_blankTitle_throwsException() {
        val income = Income(
            title = "   ",
            amount = BigDecimal("100.00"),
            source = "Salary",
            date = Instant.now()
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addIncomeUseCase(income) }
        }
        assertEquals("Title cannot be blank", ex.message)
    }

    @Test
    fun addIncomeUseCase_negativeOrZeroAmount_throwsException() {
        val zeroIncome = Income(
            title = "Zero",
            amount = BigDecimal.ZERO,
            source = "Salary",
            date = Instant.now()
        )
        val ex1 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addIncomeUseCase(zeroIncome) }
        }
        assertEquals("Amount must be greater than zero", ex1.message)

        val negativeIncome = Income(
            title = "Negative",
            amount = BigDecimal("-50.00"),
            source = "Salary",
            date = Instant.now()
        )
        val ex2 = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addIncomeUseCase(negativeIncome) }
        }
        assertEquals("Amount must be greater than zero", ex2.message)
    }

    @Test
    fun addIncomeUseCase_excessiveAmount_throwsException() {
        val hugeIncome = Income(
            title = "Lottery",
            amount = BigDecimal("100000000.01"),
            source = "Other",
            date = Instant.now()
        )
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addIncomeUseCase(hugeIncome) }
        }
        assertEquals("Amount cannot exceed 100,000,000", ex.message)
    }

    @Test
    fun addIncomeUseCase_blankSource_throwsException() {
        val income = Income(
            title = "Gift",
            amount = BigDecimal("100.00"),
            source = "   ",
            date = Instant.now()
        )
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { addIncomeUseCase(income) }
        }
        assertEquals("Income source cannot be blank", ex.message)
    }

    @Test
    fun getIncomesUseCase_returnsIncomesOrderedByDateDesc() = runBlocking {
        val now = Instant.now()
        val income1 = Income(id = 1L, title = "First", amount = BigDecimal("10.00"), source = "Salary", date = now.minus(2, ChronoUnit.DAYS))
        val income2 = Income(id = 2L, title = "Second", amount = BigDecimal("20.00"), source = "Salary", date = now)

        fakeRepository.incomes.addAll(listOf(income1, income2))

        val list = getIncomesUseCase().first()
        assertEquals(2, list.size)
        assertEquals("Second", list[0].title)
        assertEquals("First", list[1].title)
    }

    @Test
    fun getIncomeByIdUseCase_returnsCorrectIncome() = runBlocking {
        val income = Income(id = 42L, title = "Bonus", amount = BigDecimal("500.00"), source = "Salary", date = Instant.now())
        fakeRepository.incomes.add(income)

        val retrieved = getIncomeByIdUseCase(42L)
        assertNotNull(retrieved)
        assertEquals("Bonus", retrieved?.title)

        val notFound = getIncomeByIdUseCase(999L)
        assertNull(notFound)
    }

    @Test
    fun updateIncomeUseCase_validUpdate_updatesSuccessfully() = runBlocking {
        val income = Income(id = 1L, title = "Original", amount = BigDecimal("100.00"), source = "Salary", date = Instant.now())
        fakeRepository.incomes.add(income)

        val updated = income.copy(title = "Updated Title", amount = BigDecimal("150.00"), source = "business")
        updateIncomeUseCase(updated)

        val persisted = fakeRepository.incomes.find { it.id == 1L }
        assertNotNull(persisted)
        assertEquals("Updated Title", persisted?.title)
        assertEquals(BigDecimal("150.00"), persisted?.amount)
        assertEquals("Business", persisted?.source)
    }

    @Test
    fun updateIncomeUseCase_invalidUpdate_throwsException() {
        val income = Income(id = 1L, title = "Valid", amount = BigDecimal("100.00"), source = "Salary", date = Instant.now())
        fakeRepository.incomes.add(income)

        val invalid = income.copy(title = "")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { updateIncomeUseCase(invalid) }
        }
        assertEquals("Title cannot be blank", ex.message)
    }

    @Test
    fun deleteIncomeUseCase_byObjectAndId_removesIncome() = runBlocking {
        val income1 = Income(id = 1L, title = "Inc 1", amount = BigDecimal("10.00"), source = "Salary", date = Instant.now())
        val income2 = Income(id = 2L, title = "Inc 2", amount = BigDecimal("20.00"), source = "Gift", date = Instant.now())
        fakeRepository.incomes.addAll(listOf(income1, income2))

        deleteIncomeUseCase(income1)
        assertEquals(1, fakeRepository.incomes.size)
        assertEquals(2L, fakeRepository.incomes[0].id)

        deleteIncomeUseCase(2L)
        assertTrue(fakeRepository.incomes.isEmpty())
    }

    @Test
    fun getTotalIncomeUseCase_returnsAggregatedBigDecimal() = runBlocking {
        fakeRepository.incomes.add(Income(id = 1L, title = "Inc 1", amount = BigDecimal("100.50"), source = "Salary", date = Instant.now()))
        fakeRepository.incomes.add(Income(id = 2L, title = "Inc 2", amount = BigDecimal("25.25"), source = "Gift", date = Instant.now()))

        val total = getTotalIncomeUseCase().first()
        assertEquals(BigDecimal("125.75"), total)
    }

    @Test
    fun incomeSource_helpersWorkAsExpected() {
        assertTrue(IncomeSource.isBuiltIn("Salary"))
        assertTrue(IncomeSource.isBuiltIn("salary"))
        assertTrue(IncomeSource.isBuiltIn("  Freelance  "))
        assertTrue(!IncomeSource.isBuiltIn("Cryptocurrency"))

        assertEquals("Salary", IncomeSource.normalize("salary"))
        assertEquals("Investment", IncomeSource.normalize("  investment  "))
        assertEquals("Crypto Staking", IncomeSource.normalize("  Crypto Staking  "))
    }

    /**
     * In-memory fake repository implementation for fast, isolated unit testing of use cases.
     */
    private class FakeIncomeRepository : IncomeRepository {
        val incomes = mutableListOf<Income>()
        private var nextId = 1L

        override fun getAllIncomes(): Flow<List<Income>> {
            return flowOf(incomes.sortedByDescending { it.date })
        }

        override suspend fun getIncomeById(id: Long): Income? {
            return incomes.find { it.id == id }
        }

        override suspend fun insertIncome(income: Income): Long {
            val assignedId = if (income.id == 0L) nextId++ else income.id
            val stored = income.copy(id = assignedId)
            incomes.removeIf { it.id == assignedId }
            incomes.add(stored)
            return assignedId
        }

        override suspend fun updateIncome(income: Income) {
            val index = incomes.indexOfFirst { it.id == income.id }
            if (index != -1) {
                incomes[index] = income
            }
        }

        override suspend fun deleteIncome(income: Income) {
            incomes.removeIf { it.id == income.id }
        }

        override suspend fun deleteIncomeById(id: Long) {
            incomes.removeIf { it.id == id }
        }

        override fun getIncomesBySource(source: String): Flow<List<Income>> {
            return flowOf(incomes.filter { it.source == source })
        }

        override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> {
            return flowOf(incomes.filter { it.date in startDate..endDate })
        }

        override fun getTotalIncomeInCents(): Flow<Long?> {
            val total = incomes.sumOf { it.amount.movePointRight(2).toLong() }
            return flowOf(total)
        }
    }
}

