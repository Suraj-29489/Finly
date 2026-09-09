package com.personalexpensetracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.database.IncomeDao
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository
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
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit and integration tests for [IncomeRepositoryImpl] verifying domain operations,
 * bidirectional mapper conversions, and [BigDecimal] precision integrity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomeRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var incomeDao: IncomeDao
    private lateinit var repository: IncomeRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        incomeDao = database.incomeDao()
        repository = IncomeRepositoryImpl(incomeDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveIncome_preservesBigDecimalPrecision() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income = Income(
            title = "Consulting Project",
            amount = BigDecimal("1250.75"),
            source = "Business",
            date = now,
            notes = "Invoice #1024"
        )

        val id = repository.insertIncome(income)
        assertTrue(id > 0)

        val retrieved = repository.getIncomeById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved?.id)
        assertEquals("Consulting Project", retrieved?.title)
        assertEquals(BigDecimal("1250.75"), retrieved?.amount)
        assertEquals("Business", retrieved?.source)
        assertEquals(now, retrieved?.date)
        assertEquals("Invoice #1024", retrieved?.notes)
    }

    @Test
    fun getAllIncomes_mapsToDomainCorrectly() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income1 = Income(
            title = "Part-time Job",
            amount = BigDecimal("350.00"),
            source = "Salary",
            date = now.minus(1, ChronoUnit.DAYS)
        )
        val income2 = Income(
            title = "App Royalties",
            amount = BigDecimal("75.50"),
            source = "Business",
            date = now
        )

        repository.insertIncome(income1)
        repository.insertIncome(income2)

        val incomes = repository.getAllIncomes().first()
        assertEquals(2, incomes.size)
        assertEquals("App Royalties", incomes[0].title)
        assertEquals(BigDecimal("75.50"), incomes[0].amount)
        assertEquals("Part-time Job", incomes[1].title)
        assertEquals(BigDecimal("350.00"), incomes[1].amount)
    }

    @Test
    fun updateIncome_modifiesPersistedState() = runBlocking {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val income = Income(
            title = "Dividends",
            amount = BigDecimal("100.00"),
            source = "Investment",
            date = now
        )
        val id = repository.insertIncome(income)

        val updated = income.copy(
            id = id,
            title = "Q3 Stock Dividends",
            amount = BigDecimal("120.50"),
            notes = "Quarterly adjustment"
        )
        repository.updateIncome(updated)

        val retrieved = repository.getIncomeById(id)
        assertEquals("Q3 Stock Dividends", retrieved?.title)
        assertEquals(BigDecimal("120.50"), retrieved?.amount)
        assertEquals("Quarterly adjustment", retrieved?.notes)
    }

    @Test
    fun deleteIncome_removesFromPersistence() = runBlocking {
        val income = Income(
            title = "Cash Gift",
            amount = BigDecimal("20.00"),
            source = "Gift",
            date = Instant.now()
        )
        val id = repository.insertIncome(income)
        assertNotNull(repository.getIncomeById(id))

        repository.deleteIncome(income.copy(id = id))
        assertNull(repository.getIncomeById(id))
    }

    @Test
    fun deleteIncomeById_removesRecord() = runBlocking {
        val income = Income(
            title = "Old gig",
            amount = BigDecimal("45.00"),
            source = "Freelance",
            date = Instant.now()
        )
        val id = repository.insertIncome(income)
        assertNotNull(repository.getIncomeById(id))

        repository.deleteIncomeById(id)
        assertNull(repository.getIncomeById(id))
    }

    @Test
    fun getIncomesBySource_filtersCorrectly() = runBlocking {
        val now = Instant.now()
        repository.insertIncome(Income(title = "Salary A", amount = BigDecimal("2000.00"), source = "Salary", date = now))
        repository.insertIncome(Income(title = "Freelance A", amount = BigDecimal("500.00"), source = "Freelance", date = now))
        repository.insertIncome(Income(title = "Salary B", amount = BigDecimal("1000.00"), source = "Salary", date = now))

        val salaryIncomes = repository.getIncomesBySource("Salary").first()
        assertEquals(2, salaryIncomes.size)
        assertTrue(salaryIncomes.all { it.source == "Salary" })
    }

    @Test
    fun getIncomesByDateRange_filtersCorrectly() = runBlocking {
        val t1 = Instant.parse("2026-09-01T12:00:00Z")
        val t2 = Instant.parse("2026-09-05T12:00:00Z")
        val t3 = Instant.parse("2026-09-10T12:00:00Z")

        repository.insertIncome(Income(title = "Early", amount = BigDecimal("10.00"), source = "Other", date = t1))
        repository.insertIncome(Income(title = "Mid", amount = BigDecimal("20.00"), source = "Other", date = t2))
        repository.insertIncome(Income(title = "Late", amount = BigDecimal("30.00"), source = "Other", date = t3))

        val results = repository.getIncomesByDateRange(
            startDate = Instant.parse("2026-09-03T00:00:00Z"),
            endDate = Instant.parse("2026-09-07T00:00:00Z")
        ).first()

        assertEquals(1, results.size)
        assertEquals("Mid", results[0].title)
    }

    @Test
    fun getTotalIncomeInCents_reflectsAggregatedSum() = runBlocking {
        repository.insertIncome(Income(title = "Income 1", amount = BigDecimal("100.50"), source = "Salary", date = Instant.now()))
        repository.insertIncome(Income(title = "Income 2", amount = BigDecimal("50.25"), source = "Freelance", date = Instant.now()))

        val totalCents = repository.getTotalIncomeInCents().first()
        assertEquals(15075L, totalCents)
    }
}

