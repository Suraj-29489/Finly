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
 * Unit and integration tests for [IncomeDao] executed against an in-memory SQLite database via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var incomeDao: IncomeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        incomeDao = database.incomeDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveIncomeById() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income = IncomeEntity(
            title = "Monthly Salary",
            amountInCents = 500000L,
            source = "Salary",
            date = now,
            notes = "September paycheck",
            createdAt = now
        )

        val id = incomeDao.insertIncome(income)
        assertTrue(id > 0)

        val retrieved = incomeDao.getIncomeById(id)
        assertNotNull(retrieved)
        assertEquals("Monthly Salary", retrieved?.title)
        assertEquals(500000L, retrieved?.amountInCents)
        assertEquals("Salary", retrieved?.source)
        assertEquals(now, retrieved?.date)
        assertEquals("September paycheck", retrieved?.notes)
    }

    @Test
    fun insertMultipleIncomesAndGetAll() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income1 = IncomeEntity(
            title = "Freelance Project",
            amountInCents = 150000L,
            source = "Freelance",
            date = now.minus(2, ChronoUnit.DAYS),
            notes = "Web app development",
            createdAt = now
        )
        val income2 = IncomeEntity(
            title = "Stock Dividend",
            amountInCents = 25000L,
            source = "Investment",
            date = now,
            notes = "Q3 Dividend",
            createdAt = now
        )

        val ids = incomeDao.insertIncomes(listOf(income1, income2))
        assertEquals(2, ids.size)

        val allIncomes = incomeDao.getAllIncomes().first()
        assertEquals(2, allIncomes.size)
        // Ordered by date DESC
        assertEquals("Stock Dividend", allIncomes[0].title)
        assertEquals("Freelance Project", allIncomes[1].title)

        val syncIncomes = incomeDao.getAllIncomesSync()
        assertEquals(2, syncIncomes.size)
    }

    @Test
    fun updateIncome() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income = IncomeEntity(
            title = "Tutoring",
            amountInCents = 8000L,
            source = "Freelance",
            date = now,
            notes = null,
            createdAt = now
        )
        val id = incomeDao.insertIncome(income)

        val updated = income.copy(
            id = id,
            title = "Private Tutoring",
            amountInCents = 10000L,
            notes = "Updated rate"
        )
        val rowsAffected = incomeDao.updateIncome(updated)
        assertEquals(1, rowsAffected)

        val retrieved = incomeDao.getIncomeById(id)
        assertEquals("Private Tutoring", retrieved?.title)
        assertEquals(10000L, retrieved?.amountInCents)
        assertEquals("Updated rate", retrieved?.notes)
    }

    @Test
    fun deleteIncome() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income = IncomeEntity(
            title = "Gift",
            amountInCents = 5000L,
            source = "Gift",
            date = now,
            createdAt = now
        )
        val id = incomeDao.insertIncome(income)

        val rowsAffected = incomeDao.deleteIncome(income.copy(id = id))
        assertEquals(1, rowsAffected)

        val retrieved = incomeDao.getIncomeById(id)
        assertNull(retrieved)
    }

    @Test
    fun deleteIncomeById() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income = IncomeEntity(
            title = "Consulting",
            amountInCents = 60000L,
            source = "Business",
            date = now,
            createdAt = now
        )
        val id = incomeDao.insertIncome(income)

        val rowsAffected = incomeDao.deleteIncomeById(id)
        assertEquals(1, rowsAffected)

        val retrieved = incomeDao.getIncomeById(id)
        assertNull(retrieved)
    }

    @Test
    fun filterIncomesBySource() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        incomeDao.insertIncome(
            IncomeEntity(title = "Salary", amountInCents = 400000L, source = "Salary", date = now)
        )
        incomeDao.insertIncome(
            IncomeEntity(title = "App Sales", amountInCents = 50000L, source = "Business", date = now)
        )
        incomeDao.insertIncome(
            IncomeEntity(title = "Bonus", amountInCents = 100000L, source = "Salary", date = now)
        )

        val salaryIncomes = incomeDao.getIncomesBySource("Salary").first()
        assertEquals(2, salaryIncomes.size)
        assertTrue(salaryIncomes.all { it.source == "Salary" })

        val businessIncomes = incomeDao.getIncomesBySource("Business").first()
        assertEquals(1, businessIncomes.size)
        assertEquals("App Sales", businessIncomes[0].title)
    }

    @Test
    fun filterIncomesByDateRange() = runBlocking {
        val day1 = Instant.parse("2026-09-01T10:00:00Z")
        val day2 = Instant.parse("2026-09-05T10:00:00Z")
        val day3 = Instant.parse("2026-09-10T10:00:00Z")

        incomeDao.insertIncome(IncomeEntity(title = "Early", amountInCents = 1000L, source = "Other", date = day1))
        incomeDao.insertIncome(IncomeEntity(title = "Mid", amountInCents = 2000L, source = "Other", date = day2))
        incomeDao.insertIncome(IncomeEntity(title = "Late", amountInCents = 3000L, source = "Other", date = day3))

        val filtered = incomeDao.getIncomesByDateRange(
            startDate = Instant.parse("2026-09-02T00:00:00Z"),
            endDate = Instant.parse("2026-09-08T00:00:00Z")
        ).first()

        assertEquals(1, filtered.size)
        assertEquals("Mid", filtered[0].title)
    }

    @Test
    fun calculateTotalIncomeInCents() = runBlocking {
        val emptyTotal = incomeDao.getTotalIncomeInCents().first()
        assertNull(emptyTotal)

        incomeDao.insertIncome(IncomeEntity(title = "Job", amountInCents = 300000L, source = "Salary", date = Instant.now()))
        incomeDao.insertIncome(IncomeEntity(title = "Side gig", amountInCents = 50000L, source = "Freelance", date = Instant.now()))

        val total = incomeDao.getTotalIncomeInCents().first()
        assertEquals(350000L, total)
    }
}

