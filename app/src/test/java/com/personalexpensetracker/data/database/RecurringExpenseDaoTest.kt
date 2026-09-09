package com.personalexpensetracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurringExpenseDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RecurringExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.recurringExpenseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveRecurringExpenseById() = runBlocking {
        val recurring = RecurringExpenseEntity(
            title = "Gym Membership",
            amountInCents = 5000L,
            category = "Health",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-10-01",
            isActive = true,
            notes = "Monthly fitness membership"
        )

        val id = dao.insertRecurringExpense(recurring)
        assertTrue(id > 0)

        val retrieved = dao.getRecurringExpenseById(id)
        assertNotNull(retrieved)
        assertEquals(id, retrieved?.id)
        assertEquals("Gym Membership", retrieved?.title)
        assertEquals(5000L, retrieved?.amountInCents)
        assertEquals("Health", retrieved?.category)
        assertEquals("MONTHLY", retrieved?.frequency)
        assertEquals("2026-09-01", retrieved?.startDate)
        assertEquals("2026-10-01", retrieved?.nextOccurrenceDate)
        assertTrue(retrieved?.isActive == true)
    }

    @Test
    fun getActiveRecurringExpensesReturnsOnlyActive() = runBlocking {
        val active = RecurringExpenseEntity(
            title = "Netflix",
            amountInCents = 1599L,
            category = "Entertainment",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-10-01",
            isActive = true
        )
        val paused = RecurringExpenseEntity(
            title = "Spotify",
            amountInCents = 999L,
            category = "Entertainment",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-10-01",
            isActive = false
        )

        dao.insertRecurringExpenses(listOf(active, paused))

        val all = dao.getAllRecurringExpenses().first()
        assertEquals(2, all.size)

        val activeList = dao.getActiveRecurringExpenses().first()
        assertEquals(1, activeList.size)
        assertEquals("Netflix", activeList[0].title)
    }

    @Test
    fun getDueRecurringExpensesFiltersCorrectly() = runBlocking {
        val pastDue = RecurringExpenseEntity(
            title = "Rent",
            amountInCents = 120000L,
            category = "Bills",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-09-01",
            isActive = true
        )
        val future = RecurringExpenseEntity(
            title = "Internet",
            amountInCents = 6000L,
            category = "Bills",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-09-15",
            isActive = true
        )
        val pausedDue = RecurringExpenseEntity(
            title = "Water",
            amountInCents = 3000L,
            category = "Bills",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-09-01",
            isActive = false
        )

        dao.insertRecurringExpenses(listOf(pastDue, future, pausedDue))

        val dueOnSep7 = dao.getDueRecurringExpenses("2026-09-07")
        assertEquals(1, dueOnSep7.size)
        assertEquals("Rent", dueOnSep7[0].title)
    }

    @Test
    fun setActiveTogglesStatus() = runBlocking {
        val recurring = RecurringExpenseEntity(
            title = "Storage Unit",
            amountInCents = 7500L,
            category = "Other",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-10-01",
            isActive = true
        )
        val id = dao.insertRecurringExpense(recurring)

        dao.setActive(id, false, Instant.now())
        val paused = dao.getRecurringExpenseById(id)
        assertNotNull(paused)
        assertFalse(paused!!.isActive)

        dao.setActive(id, true, Instant.now())
        val resumed = dao.getRecurringExpenseById(id)
        assertNotNull(resumed)
        assertTrue(resumed!!.isActive)
    }

    @Test
    fun updateOccurrenceAdvancesSchedule() = runBlocking {
        val recurring = RecurringExpenseEntity(
            title = "Phone Bill",
            amountInCents = 4500L,
            category = "Bills",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-09-01",
            isActive = true
        )
        val id = dao.insertRecurringExpense(recurring)

        dao.updateOccurrence(id, "2026-10-01", "2026-09-01", Instant.now())

        val updated = dao.getRecurringExpenseById(id)
        assertNotNull(updated)
        assertEquals("2026-10-01", updated?.nextOccurrenceDate)
        assertEquals("2026-09-01", updated?.lastGeneratedDate)
    }

    @Test
    fun deleteRecurringExpenseRemovesEntity() = runBlocking {
        val recurring = RecurringExpenseEntity(
            title = "Magazine",
            amountInCents = 1000L,
            category = "Entertainment",
            frequency = "MONTHLY",
            startDate = "2026-09-01",
            nextOccurrenceDate = "2026-10-01"
        )
        val id = dao.insertRecurringExpense(recurring)
        dao.deleteRecurringExpenseById(id)

        assertNull(dao.getRecurringExpenseById(id))
    }
}

