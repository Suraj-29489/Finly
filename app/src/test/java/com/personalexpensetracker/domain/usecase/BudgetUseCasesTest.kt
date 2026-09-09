package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

class BudgetUseCasesTest {

    private lateinit var fakeRepository: FakeBudgetRepository
    private val month = YearMonth.of(2026, 9)

    @Before
    fun setUp() {
        fakeRepository = FakeBudgetRepository()
    }

    @Test
    fun setMonthlyBudgetUseCaseSucceedsForValidAmount() = runBlocking {
        val useCase = SetMonthlyBudgetUseCase(fakeRepository)
        val id = useCase(month, BigDecimal("1200.00"))
        assertTrue(id > 0)
        assertEquals(BigDecimal("1200.00"), fakeRepository.monthlyBudget?.amount)
    }

    @Test
    fun setMonthlyBudgetUseCaseThrowsForInvalidAmount() {
        val useCase = SetMonthlyBudgetUseCase(fakeRepository)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCase(month, BigDecimal("-10.00"))
            }
        }
    }

    @Test
    fun setCategoryBudgetUseCaseNormalizesCategoryAndSucceeds() = runBlocking {
        val useCase = SetCategoryBudgetUseCase(fakeRepository)
        val id = useCase(month, "food ", BigDecimal("350.00"))
        assertTrue(id > 0)
        val saved = fakeRepository.categoryBudgets["Food"]
        assertNotNull(saved)
        assertEquals("Food", saved?.category) // Normalized to canonical Food
        assertEquals(BigDecimal("350.00"), saved?.amount)
    }

    @Test
    fun setCategoryBudgetUseCaseThrowsForBlankCategory() {
        val useCase = SetCategoryBudgetUseCase(fakeRepository)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCase(month, "   ", BigDecimal("350.00"))
            }
        }
    }

    @Test
    fun getMonthlyBudgetUseCaseReturnsFlow() = runBlocking {
        fakeRepository.monthlyBudget = Budget(id = 1L, category = null, amount = BigDecimal("1500.00"), month = month)
        val useCase = GetMonthlyBudgetUseCase(fakeRepository)
        val result = useCase(month).first()
        assertNotNull(result)
        assertEquals(BigDecimal("1500.00"), result?.amount)
    }

    @Test
    fun getCategoryBudgetsUseCaseReturnsFlow() = runBlocking {
        fakeRepository.categoryBudgets["Food"] = Budget(id = 2L, category = "Food", amount = BigDecimal("400.00"), month = month)
        val useCase = GetCategoryBudgetsUseCase(fakeRepository)
        val result = useCase(month).first()
        assertEquals(1, result.size)
        assertEquals("Food", result[0].category)
    }

    @Test
    fun deleteBudgetUseCaseDelegatesCorrectly() = runBlocking {
        fakeRepository.monthlyBudget = Budget(id = 1L, category = null, amount = BigDecimal("1500.00"), month = month)
        val deleteUseCase = DeleteBudgetUseCase(fakeRepository)
        deleteUseCase.monthlyBudget(month)
        assertTrue(fakeRepository.monthlyBudgetDeleted)
    }

    private class FakeBudgetRepository : BudgetRepository {
        var monthlyBudget: Budget? = null
        val categoryBudgets = mutableMapOf<String, Budget>()
        var monthlyBudgetDeleted = false

        override fun getAllBudgets(): Flow<List<Budget>> = flowOf(listOfNotNull(monthlyBudget) + categoryBudgets.values)
        override fun getBudgetsForMonth(month: YearMonth): Flow<List<Budget>> = flowOf(listOfNotNull(monthlyBudget) + categoryBudgets.values)
        override fun getMonthlyBudget(month: YearMonth): Flow<Budget?> = flowOf(monthlyBudget)
        override fun getCategoryBudgets(month: YearMonth): Flow<List<Budget>> = flowOf(categoryBudgets.values.toList())
        override fun getCategoryBudget(month: YearMonth, category: String): Flow<Budget?> = flowOf(categoryBudgets[category])
        override suspend fun getBudgetById(id: Long): Budget? = null

        override suspend fun setMonthlyBudget(month: YearMonth, amount: BigDecimal): Long {
            monthlyBudget = Budget(id = 1L, category = null, amount = amount, month = month)
            return 1L
        }

        override suspend fun setCategoryBudget(month: YearMonth, category: String, amount: BigDecimal): Long {
            val budget = Budget(id = (categoryBudgets.size + 2).toLong(), category = category, amount = amount, month = month)
            categoryBudgets[category] = budget
            return budget.id
        }

        override suspend fun insertBudget(budget: Budget): Long = 1L
        override suspend fun updateBudget(budget: Budget) {}
        override suspend fun deleteBudget(budget: Budget) {}
        override suspend fun deleteBudgetById(id: Long) {}
        override suspend fun deleteMonthlyBudget(month: YearMonth) {
            monthlyBudget = null
            monthlyBudgetDeleted = true
        }
        override suspend fun deleteCategoryBudget(month: YearMonth, category: String) {
            categoryBudgets.remove(category)
        }
    }
}

