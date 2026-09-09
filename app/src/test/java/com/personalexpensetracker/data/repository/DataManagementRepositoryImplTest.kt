package com.personalexpensetracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataManagementRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DataManagementRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DataManagementRepositoryImpl(database)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun getFullFinancialDataReturnsAllStoredEntities() = runBlocking {
        val sampleExpense = Expense(0L, "Coffee", BigDecimal("4.50"), "Food", Instant.parse("2026-09-01T10:00:00Z"))
        val sampleIncome = Income(0L, "Paycheck", BigDecimal("2000.00"), "Salary", Instant.parse("2026-09-01T10:00:00Z"))
        val sampleBudget = Budget(id = 0L, category = null, amount = BigDecimal("500.00"), month = YearMonth.of(2026, 9))
        val sampleRecurring = RecurringExpense(
            0L, "Subscription", BigDecimal("15.00"), "Entertainment",
            RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)
        )

        database.expenseDao().insertExpense(sampleExpense.toEntity())
        database.incomeDao().insertIncome(sampleIncome.toEntity())
        database.budgetDao().insertBudget(sampleBudget.toEntity())
        database.recurringExpenseDao().insertRecurringExpense(sampleRecurring.toEntity())

        val snapshot = repository.getFullFinancialData()

        assertEquals(1, snapshot.expenses.size)
        assertEquals("Coffee", snapshot.expenses.first().title)
        assertEquals(BigDecimal("4.50"), snapshot.expenses.first().amount)

        assertEquals(1, snapshot.incomes.size)
        assertEquals("Paycheck", snapshot.incomes.first().title)
        assertEquals(BigDecimal("2000.00"), snapshot.incomes.first().amount)

        assertEquals(1, snapshot.budgets.size)
        assertEquals(BigDecimal("500.00"), snapshot.budgets.first().amount)

        assertEquals(1, snapshot.recurringExpenses.size)
        assertEquals("Subscription", snapshot.recurringExpenses.first().title)
    }

    @Test
    fun restoreFinancialDataReplacesExistingDataAtomically() = runBlocking {
        // Pre-populate database with old data
        database.expenseDao().insertExpense(Expense(0L, "Old Expense", BigDecimal("10.00"), "Misc", Instant.now()).toEntity())
        database.incomeDao().insertIncome(Income(0L, "Old Income", BigDecimal("20.00"), "Misc", Instant.now()).toEntity())

        val backup = FinancialBackupData(
            version = 1,
            exportedAt = Instant.now(),
            expenses = listOf(
                Expense(0L, "New Expense 1", BigDecimal("50.00"), "Shopping", Instant.parse("2026-09-02T10:00:00Z")),
                Expense(0L, "New Expense 2", BigDecimal("25.00"), "Food", Instant.parse("2026-09-03T10:00:00Z"))
            ),
            incomes = listOf(
                Income(0L, "New Salary", BigDecimal("3500.00"), "Salary", Instant.parse("2026-09-01T10:00:00Z"))
            ),
            budgets = listOf(
                Budget(id = 0L, category = null, amount = BigDecimal("1000.00"), month = YearMonth.of(2026, 9))
            ),
            recurringExpenses = emptyList()
        )

        val summary = repository.restoreFinancialData(backup)

        assertEquals(2, summary.expensesImported)
        assertEquals(1, summary.incomesImported)
        assertEquals(1, summary.budgetsImported)
        assertEquals(0, summary.recurringExpensesImported)

        val currentData = repository.getFullFinancialData()
        assertEquals(2, currentData.expenses.size)
        assertTrue(currentData.expenses.none { it.title == "Old Expense" })
        assertEquals(1, currentData.incomes.size)
        assertEquals("New Salary", currentData.incomes.first().title)
    }

    @Test
    fun importFinancialDataMergesAndSkipsDuplicates() = runBlocking {
        val existingDate = Instant.parse("2026-09-01T10:00:00Z")
        database.expenseDao().insertExpense(Expense(0L, "Existing Expense", BigDecimal("10.00"), "Misc", existingDate).toEntity())

        val importData = FinancialBackupData(
            expenses = listOf(
                Expense(0L, "Existing Expense", BigDecimal("10.00"), "Misc", existingDate), // Duplicate!
                Expense(0L, "Brand New Expense", BigDecimal("75.00"), "Travel", Instant.parse("2026-09-05T10:00:00Z"))
            )
        )

        val summary = repository.importFinancialData(importData, preventDuplicates = true)

        assertEquals(1, summary.expensesImported)
        assertEquals(1, summary.skippedRecords)

        val currentData = repository.getFullFinancialData()
        assertEquals(2, currentData.expenses.size)
    }

    @Test
    fun clearAllDataWipesAllTables() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Expense", BigDecimal("10.00"), "Misc", Instant.now()).toEntity())
        database.incomeDao().insertIncome(Income(0L, "Income", BigDecimal("20.00"), "Misc", Instant.now()).toEntity())

        repository.clearAllData()

        val data = repository.getFullFinancialData()
        assertTrue(data.expenses.isEmpty())
        assertTrue(data.incomes.isEmpty())
        assertTrue(data.budgets.isEmpty())
        assertTrue(data.recurringExpenses.isEmpty())
    }

    @Test
    fun importFromCsvImportsRecordsAndPreventsDuplicates() = runBlocking {
        val csv = """
            Title,Amount,Category,Date,Notes
            Groceries,54.20,Food,2026-09-02T10:00:00Z,Weekly shopping
            Gas,35.00,Transportation,2026-09-02T15:00:00Z,Fuel
        """.trimIndent()

        val summary1 = repository.importFromCsv(csv, preventDuplicates = true)
        assertEquals(2, summary1.expensesImported)
        assertEquals(0, summary1.skippedRecords)
        assertEquals(0, summary1.failedRecords)

        // Second import of same CSV should skip both as duplicates
        val summary2 = repository.importFromCsv(csv, preventDuplicates = true)
        assertEquals(0, summary2.expensesImported)
        assertEquals(2, summary2.skippedRecords)
        assertEquals(0, summary2.failedRecords)
    }

    @Test
    fun importFromJsonImportsRecordsAndHandlesMalformedPayloads() = runBlocking {
        val validJson = """
        {
            "version": 1,
            "expenses": [
                {
                    "title": "Hotel Stay",
                    "amount": "250.00",
                    "category": "Travel",
                    "date": "2026-09-03T10:00:00Z"
                }
            ],
            "incomes": [
                {
                    "title": "Consulting Bonus",
                    "amount": "1000.00",
                    "source": "Business",
                    "date": "2026-09-03T11:00:00Z"
                }
            ]
        }
        """.trimIndent()

        val summary = repository.importFromJson(validJson)
        assertEquals(1, summary.expensesImported)
        assertEquals(1, summary.incomesImported)
        assertEquals(0, summary.failedRecords)

        val malformedJson = "{ invalid: json }"
        val malformedSummary = repository.importFromJson(malformedJson)
        assertEquals(1, malformedSummary.failedRecords)
        assertTrue(malformedSummary.errors.isNotEmpty())
    }

    @Test
    fun createAndRestoreBackupBundle_unencrypted_restoresDataCorrectly() = runBlocking {
        // 1. Populate
        database.expenseDao().insertExpense(Expense(0L, "Coffee", BigDecimal("4.00"), "Food", Instant.now()).toEntity())

        // 2. Backup
        val backupString = repository.createBackup()

        // 3. Clear DB
        repository.clearAllData()
        assertTrue(repository.getFullFinancialData().expenses.isEmpty())

        // 4. Restore
        val summary = repository.restoreFromBackupBundle(backupString)
        assertEquals(1, summary.expensesImported)
        assertEquals(0, summary.failedRecords)

        val restored = repository.getFullFinancialData()
        assertEquals(1, restored.expenses.size)
        assertEquals("Coffee", restored.expenses[0].title)
    }

    @Test
    fun createAndRestoreBackupBundle_encrypted_restoresDataWithCorrectPassword() = runBlocking {
        val password = "MySecretPassword123".toCharArray()
        database.incomeDao().insertIncome(Income(0L, "Salary", BigDecimal("3000.00"), "Job", Instant.now()).toEntity())

        val encryptedBackup = repository.createBackup(password)
        assertTrue(encryptedBackup.contains("FINLY_ENCRYPTED_BACKUP"))

        repository.clearAllData()

        val summary = repository.restoreFromBackupBundle(encryptedBackup, password)
        assertEquals(1, summary.incomesImported)
        assertEquals(0, summary.failedRecords)

        val restored = repository.getFullFinancialData()
        assertEquals(1, restored.incomes.size)
        assertEquals("Salary", restored.incomes[0].title)
    }

    @Test
    fun restoreFromBackupBundle_wrongPassword_leavesExistingDataIntact() = runBlocking {
        val originalExpense = Expense(0L, "Original Expense", BigDecimal("25.00"), "Food", Instant.now())
        database.expenseDao().insertExpense(originalExpense.toEntity())

        val password = "CorrectPassword".toCharArray()
        val encryptedBackup = repository.createBackup(password)

        // Attempt restore with wrong password
        val wrongPassword = "WrongPassword".toCharArray()
        val summary = repository.restoreFromBackupBundle(encryptedBackup, wrongPassword)

        assertEquals(1, summary.failedRecords)
        assertTrue(summary.errors.first().contains("Incorrect password or corrupted backup file"))

        // Verify existing data remains untouched
        val current = repository.getFullFinancialData()
        assertEquals(1, current.expenses.size)
        assertEquals("Original Expense", current.expenses[0].title)
    }

    @Test
    fun restoreFromBackupBundle_corruptedChecksum_leavesExistingDataIntact() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Original Expense", BigDecimal("25.00"), "Food", Instant.now()).toEntity())

        val backup = repository.createBackup()
        // Corrupt content
        val corrupted = backup.replace("25.00", "99999.00")

        val summary = repository.restoreFromBackupBundle(corrupted)
        assertEquals(1, summary.failedRecords)
        assertTrue(summary.errors.first().contains("integrity check failed"))

        // Existing data remains untouched
        val current = repository.getFullFinancialData()
        assertEquals(1, current.expenses.size)
        assertEquals("Original Expense", current.expenses[0].title)
        assertTrue(BigDecimal("25.00").compareTo(current.expenses[0].amount) == 0)
    }
}

