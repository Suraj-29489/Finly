package com.personalexpensetracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
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
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurringExpenseRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RecurringExpenseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RecurringExpenseRepositoryImpl(database.recurringExpenseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveRecurringExpense() = runBlocking {
        val recurring = RecurringExpense(
            title = "Cloud Storage",
            amount = BigDecimal("9.99"),
            category = "Bills",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 10, 1)
        )

        val id = repository.insertRecurringExpense(recurring)
        assertTrue(id > 0)

        val retrieved = repository.getRecurringExpenseById(id)
        assertNotNull(retrieved)
        assertEquals("Cloud Storage", retrieved?.title)
        assertEquals(BigDecimal("9.99"), retrieved?.amount)
        assertEquals(RecurrenceFrequency.MONTHLY, retrieved?.frequency)
        assertEquals(LocalDate.of(2026, 10, 1), retrieved?.nextOccurrenceDate)
    }

    @Test
    fun getActiveAndDueRecurringExpenses() = runBlocking {
        val dueToday = RecurringExpense(
            title = "Daily Commute",
            amount = BigDecimal("4.50"),
            category = "Transportation",
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 9, 7),
            isActive = true
        )
        val future = RecurringExpense(
            title = "Annual Insurance",
            amount = BigDecimal("850.00"),
            category = "Bills",
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 12, 1),
            isActive = true
        )
        val paused = RecurringExpense(
            title = "Paused Music",
            amount = BigDecimal("10.00"),
            category = "Entertainment",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 9, 5),
            isActive = false
        )

        repository.insertRecurringExpense(dueToday)
        repository.insertRecurringExpense(future)
        repository.insertRecurringExpense(paused)

        val active = repository.getActiveRecurringExpenses().first()
        assertEquals(2, active.size)

        val due = repository.getDueRecurringExpenses(LocalDate.of(2026, 9, 7))
        assertEquals(1, due.size)
        assertEquals("Daily Commute", due[0].title)
    }

    @Test
    fun setActivePausesAndResumes() = runBlocking {
        val recurring = RecurringExpense(
            title = "Gym",
            amount = BigDecimal("40.00"),
            category = "Health",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 10, 1),
            isActive = true
        )
        val id = repository.insertRecurringExpense(recurring)

        repository.setActive(id, false)
        val paused = repository.getRecurringExpenseById(id)
        assertFalse(paused!!.isActive)

        repository.setActive(id, true)
        val resumed = repository.getRecurringExpenseById(id)
        assertTrue(resumed!!.isActive)
    }

    @Test
    fun updateNextOccurrenceAdvancesDates() = runBlocking {
        val recurring = RecurringExpense(
            title = "Rent",
            amount = BigDecimal("1200.00"),
            category = "Bills",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 9, 1)
        )
        val id = repository.insertRecurringExpense(recurring)

        repository.updateNextOccurrence(
            id = id,
            nextOccurrenceDate = LocalDate.of(2026, 10, 1),
            lastGeneratedDate = LocalDate.of(2026, 9, 1)
        )

        val updated = repository.getRecurringExpenseById(id)
        assertEquals(LocalDate.of(2026, 10, 1), updated?.nextOccurrenceDate)
        assertEquals(LocalDate.of(2026, 9, 1), updated?.lastGeneratedDate)
    }

    @Test
    fun deleteRecurringExpenseByIdRemovesRecord() = runBlocking {
        val recurring = RecurringExpense(
            title = "Sub",
            amount = BigDecimal("5.00"),
            category = "Other",
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2026, 9, 1),
            nextOccurrenceDate = LocalDate.of(2026, 9, 8)
        )
        val id = repository.insertRecurringExpense(recurring)
        repository.deleteRecurringExpenseById(id)

        assertNull(repository.getRecurringExpenseById(id))
    }
}

