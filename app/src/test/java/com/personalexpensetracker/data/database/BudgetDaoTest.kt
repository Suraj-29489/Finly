package com.personalexpensetracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit and integration tests for [BudgetDao] executed against an in-memory SQLite database via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var budgetDao: BudgetDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        budgetDao = database.budgetDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveBudgetById() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val budget = BudgetEntity(
            category = null,
            amountInCents = 150000L, // $1500.00
            month = "2026-09",
            createdAt = now,
            updatedAt = now
        )

        val id = budgetDao.insertBudget(budget)
        assertTrue(id > 0)

        val retrieved = budgetDao.getBudgetById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved?.id)
        assertNull(retrieved?.category)
        assertEquals(150000L, retrieved?.amountInCents)
        assertEquals("2026-09", retrieved?.month)
    }

    @Test
    fun getMonthlyBudgetReturnsOnlyOverallBudget() = runBlocking {
        val overallBudget = BudgetEntity(
            category = null,
            amountInCents = 200000L,
            month = "2026-09"
        )
        val categoryBudget = BudgetEntity(
            category = "Food",
            amountInCents = 50000L,
            month = "2026-09"
        )

        budgetDao.insertBudgets(listOf(overallBudget, categoryBudget))

        val monthly = budgetDao.getMonthlyBudget("2026-09").first()
        assertNotNull(monthly)
        assertNull(monthly?.category)
        assertEquals(200000L, monthly?.amountInCents)
    }

    @Test
    fun getCategoryBudgetReturnsSpecificCategory() = runBlocking {
        val foodBudget = BudgetEntity(category = "Food", amountInCents = 40000L, month = "2026-09")
        val transportBudget = BudgetEntity(category = "Transportation", amountInCents = 20000L, month = "2026-09")

        budgetDao.insertBudgets(listOf(foodBudget, transportBudget))

        val retrievedFood = budgetDao.getCategoryBudget("2026-09", "Food").first()
        assertNotNull(retrievedFood)
        assertEquals("Food", retrievedFood?.category)
        assertEquals(40000L, retrievedFood?.amountInCents)

        val retrievedTransport = budgetDao.getCategoryBudget("2026-09", "Transportation").first()
        assertNotNull(retrievedTransport)
        assertEquals("Transportation", retrievedTransport?.category)
        assertEquals(20000L, retrievedTransport?.amountInCents)

        val missing = budgetDao.getCategoryBudget("2026-09", "Health").first()
        assertNull(missing)
    }

    @Test
    fun getCategoryBudgetsExcludesOverallBudget() = runBlocking {
        val overall = BudgetEntity(category = null, amountInCents = 300000L, month = "2026-09")
        val food = BudgetEntity(category = "Food", amountInCents = 60000L, month = "2026-09")
        val bills = BudgetEntity(category = "Bills", amountInCents = 80000L, month = "2026-09")

        budgetDao.insertBudgets(listOf(overall, food, bills))

        val categoryBudgets = budgetDao.getCategoryBudgets("2026-09").first()
        assertEquals(2, categoryBudgets.size)
        assertTrue(categoryBudgets.none { it.category == null })
        assertEquals("Bills", categoryBudgets[0].category) // Ordered alphabetically
        assertEquals("Food", categoryBudgets[1].category)
    }

    @Test
    fun updateBudgetModifiesRecord() = runBlocking {
        val budget = BudgetEntity(
            category = "Food",
            amountInCents = 30000L,
            month = "2026-09"
        )
        val id = budgetDao.insertBudget(budget)

        val updated = budget.copy(
            id = id,
            amountInCents = 45000L,
            updatedAt = Instant.now()
        )
        val rowsAffected = budgetDao.updateBudget(updated)
        assertEquals(1, rowsAffected)

        val retrieved = budgetDao.getBudgetById(id)
        assertEquals(45000L, retrieved?.amountInCents)
    }

    @Test
    fun deleteBudgetRemovesEntity() = runBlocking {
        val budget = BudgetEntity(
            category = null,
            amountInCents = 100000L,
            month = "2026-09"
        )
        val id = budgetDao.insertBudget(budget)
        val inserted = budgetDao.getBudgetById(id)
        assertNotNull(inserted)

        val rowsDeleted = budgetDao.deleteBudget(inserted!!)
        assertEquals(1, rowsDeleted)

        val retrievedAfterDelete = budgetDao.getBudgetById(id)
        assertNull(retrievedAfterDelete)
    }

    @Test
    fun deleteMonthlyBudgetsRemovesOnlyOverallForMonth() = runBlocking {
        val overall = BudgetEntity(category = null, amountInCents = 200000L, month = "2026-09")
        val category = BudgetEntity(category = "Food", amountInCents = 50000L, month = "2026-09")

        budgetDao.insertBudgets(listOf(overall, category))

        budgetDao.deleteMonthlyBudget("2026-09")

        assertNull(budgetDao.getMonthlyBudget("2026-09").first())
        assertNotNull(budgetDao.getCategoryBudget("2026-09", "Food").first())
    }

    @Test
    fun deleteCategoryBudgetRemovesOnlyTargetCategory() = runBlocking {
        val food = BudgetEntity(category = "Food", amountInCents = 50000L, month = "2026-09")
        val bills = BudgetEntity(category = "Bills", amountInCents = 70000L, month = "2026-09")

        budgetDao.insertBudgets(listOf(food, bills))

        budgetDao.deleteCategoryBudget("2026-09", "Food")

        assertNull(budgetDao.getCategoryBudget("2026-09", "Food").first())
        assertNotNull(budgetDao.getCategoryBudget("2026-09", "Bills").first())
    }
}

