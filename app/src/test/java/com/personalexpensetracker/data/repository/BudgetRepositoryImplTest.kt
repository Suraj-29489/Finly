package com.personalexpensetracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetRepositoryImpl(database.budgetDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun setMonthlyBudgetInsertsWhenNotPresent() = runBlocking {
        val month = YearMonth.of(2026, 9)
        val id = repository.setMonthlyBudget(month, BigDecimal("1200.00"))
        assertTrue(id > 0)

        val retrieved = repository.getMonthlyBudget(month).first()
        assertNotNull(retrieved)
        assertEquals(BigDecimal("1200.00"), retrieved?.amount)
        assertNull(retrieved?.category)
        assertTrue(retrieved?.isOverall == true)
    }

    @Test
    fun setMonthlyBudgetUpdatesWhenAlreadyPresent() = runBlocking {
        val month = YearMonth.of(2026, 9)
        val id1 = repository.setMonthlyBudget(month, BigDecimal("1000.00"))

        val id2 = repository.setMonthlyBudget(month, BigDecimal("1500.00"))
        assertEquals(id1, id2) // Same ID updated

        val retrieved = repository.getMonthlyBudget(month).first()
        assertEquals(BigDecimal("1500.00"), retrieved?.amount)

        val allForMonth = repository.getBudgetsForMonth(month).first()
        assertEquals(1, allForMonth.size)
    }

    @Test
    fun setCategoryBudgetInsertsAndUpdatesCorrectly() = runBlocking {
        val month = YearMonth.of(2026, 9)
        val id1 = repository.setCategoryBudget(month, "Food", BigDecimal("400.00"))
        assertTrue(id1 > 0)

        val retrieved = repository.getCategoryBudget(month, "Food").first()
        assertNotNull(retrieved)
        assertEquals("Food", retrieved?.category)
        assertEquals(BigDecimal("400.00"), retrieved?.amount)
        assertTrue(retrieved?.isCategoryBudget == true)

        // Update amount
        val id2 = repository.setCategoryBudget(month, "Food", BigDecimal("450.00"))
        assertEquals(id1, id2)

        val updated = repository.getCategoryBudget(month, "Food").first()
        assertEquals(BigDecimal("450.00"), updated?.amount)
    }

    @Test
    fun getCategoryBudgetsReturnsAllCategoryBudgetsForMonth() = runBlocking {
        val month = YearMonth.of(2026, 9)
        repository.setMonthlyBudget(month, BigDecimal("2000.00"))
        repository.setCategoryBudget(month, "Food", BigDecimal("500.00"))
        repository.setCategoryBudget(month, "Transportation", BigDecimal("300.00"))

        val categoryBudgets = repository.getCategoryBudgets(month).first()
        assertEquals(2, categoryBudgets.size)
        assertEquals("Food", categoryBudgets[0].category)
        assertEquals("Transportation", categoryBudgets[1].category)

        val allBudgets = repository.getBudgetsForMonth(month).first()
        assertEquals(3, allBudgets.size)
    }

    @Test
    fun deleteMonthlyBudgetsRemovesOnlyOverall() = runBlocking {
        val month = YearMonth.of(2026, 9)
        repository.setMonthlyBudget(month, BigDecimal("2000.00"))
        repository.setCategoryBudget(month, "Food", BigDecimal("500.00"))

        repository.deleteMonthlyBudget(month)

        assertNull(repository.getMonthlyBudget(month).first())
        assertNotNull(repository.getCategoryBudget(month, "Food").first())
    }

    @Test
    fun deleteCategoryBudgetRemovesOnlyTargetCategory() = runBlocking {
        val month = YearMonth.of(2026, 9)
        repository.setCategoryBudget(month, "Food", BigDecimal("500.00"))
        repository.setCategoryBudget(month, "Bills", BigDecimal("700.00"))

        repository.deleteCategoryBudget(month, "Food")

        assertNull(repository.getCategoryBudget(month, "Food").first())
        assertNotNull(repository.getCategoryBudget(month, "Bills").first())
    }
}

