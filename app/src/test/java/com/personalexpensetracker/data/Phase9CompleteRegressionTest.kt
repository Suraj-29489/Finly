package com.personalexpensetracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.export.CsvExporter
import com.personalexpensetracker.data.export.CsvImporter
import com.personalexpensetracker.data.export.JsonDataExporter
import com.personalexpensetracker.data.repository.DataManagementRepositoryImpl
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import com.personalexpensetracker.data.repository.IncomeRepositoryImpl
import com.personalexpensetracker.data.repository.toEntity
import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.usecase.GetFinancialBalanceUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase9CompleteRegressionTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DataManagementRepositoryImpl
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private lateinit var incomeRepo: IncomeRepositoryImpl
    private lateinit var balanceUseCase: GetFinancialBalanceUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DataManagementRepositoryImpl(database)
        expenseRepo = ExpenseRepositoryImpl(database.expenseDao())
        incomeRepo = IncomeRepositoryImpl(database.incomeDao())
        balanceUseCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // TEST 1: Create expenses and income. Export to CSV. Verify exported data.
    @Test
    fun test1_exportCsv_verifyData() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Coffee & Donut", BigDecimal("7.50"), "Food", Instant.parse("2026-09-01T10:00:00Z"), "Morning snack").toEntity())
        database.incomeDao().insertIncome(Income(0L, "Side Project", BigDecimal("850.00"), "Freelance", Instant.parse("2026-09-01T12:00:00Z"), "Milestone completed").toEntity())

        val data = repository.getFullFinancialData()
        val expensesCsv = CsvExporter.exportExpensesToCsv(data.expenses)
        val incomesCsv = CsvExporter.exportIncomesToCsv(data.incomes)
        val combinedCsv = CsvExporter.exportCombinedToCsv(data.expenses, data.incomes)

        assertTrue(expensesCsv.contains("Coffee & Donut"))
        assertTrue(expensesCsv.contains("7.50"))
        assertTrue(incomesCsv.contains("Side Project"))
        assertTrue(incomesCsv.contains("850.00"))
        assertTrue(combinedCsv.contains("EXPENSE") && combinedCsv.contains("INCOME"))
    }

    // TEST 2: Create financial data. Export to JSON. Import JSON into test DB. Verify equivalent data.
    @Test
    fun test2_exportJson_importJson_equivalentData() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Sneakers", BigDecimal("120.00"), "Shopping", Instant.parse("2026-09-02T15:00:00Z")).toEntity())
        database.incomeDao().insertIncome(Income(0L, "Dividend", BigDecimal("45.00"), "Investment", Instant.parse("2026-09-02T16:00:00Z")).toEntity())

        val exportedJson = JsonDataExporter.exportToJson(repository.getFullFinancialData())
        repository.clearAllData()
        assertTrue(repository.getFullFinancialData().expenses.isEmpty())

        val summary = repository.importFromJson(exportedJson)
        assertEquals(1, summary.expensesImported)
        assertEquals(1, summary.incomesImported)

        val restored = repository.getFullFinancialData()
        assertEquals("Sneakers", restored.expenses[0].title)
        assertEquals("Dividend", restored.incomes[0].title)
    }

    // TEST 3: Create data. Create backup. Clear DB. Restore backup. Verify records.
    @Test
    fun test3_backup_restore_unencrypted() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Dinner", BigDecimal("65.00"), "Food", Instant.now()).toEntity())
        database.budgetDao().insertBudget(Budget(0L, "Food", BigDecimal("400.00"), YearMonth.of(2026, 9)).toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()

        val summary = repository.restoreFromBackupBundle(backup)
        assertEquals(1, summary.expensesImported)
        assertEquals(1, summary.budgetsImported)

        val restored = repository.getFullFinancialData()
        assertEquals(1, restored.expenses.size)
        assertEquals(1, restored.budgets.size)
    }

    // TEST 4: Create encrypted backup. Restore with correct credentials.
    @Test
    fun test4_encryptedBackup_restore_correctPassword() = runBlocking {
        val password = "StrongPassword#2026".toCharArray()
        database.expenseDao().insertExpense(Expense(0L, "Watch", BigDecimal("350.00"), "Shopping", Instant.now()).toEntity())

        val encryptedBackup = repository.createBackup(password)
        repository.clearAllData()

        val summary = repository.restoreFromBackupBundle(encryptedBackup, password)
        assertEquals(1, summary.expensesImported)
        assertEquals(0, summary.failedRecords)

        val restored = repository.getFullFinancialData()
        assertEquals("Watch", restored.expenses[0].title)
    }

    // TEST 5: Attempt restore with incorrect credentials. Verify existing data untouched.
    @Test
    fun test5_encryptedBackup_restore_incorrectPassword_untouched() = runBlocking {
        val original = Expense(0L, "Existing Item", BigDecimal("15.00"), "Food", Instant.now())
        database.expenseDao().insertExpense(original.toEntity())

        val backup = repository.createBackup("Password123".toCharArray())

        val summary = repository.restoreFromBackupBundle(backup, "WrongPassword!".toCharArray())
        assertEquals(1, summary.failedRecords)

        val current = repository.getFullFinancialData()
        assertEquals(1, current.expenses.size)
        assertEquals("Existing Item", current.expenses[0].title)
    }

    // TEST 6: Corrupt backup. Attempt restore. Safe failure without corruption.
    @Test
    fun test6_corruptedBackup_restore_safeFailure() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Protected", BigDecimal("99.00"), "Misc", Instant.now()).toEntity())

        val backup = repository.createBackup()
        val corrupted = backup.replace("Protected", "Corrupted").replace("99.00", "0.00")

        val summary = repository.restoreFromBackupBundle(corrupted)
        assertEquals(1, summary.failedRecords)
        assertTrue(summary.errors.first().contains("integrity check failed"))

        val current = repository.getFullFinancialData()
        assertEquals(1, current.expenses.size)
        assertEquals("Protected", current.expenses[0].title)
    }

    // TEST 7: Import malformed CSV. Invalid records rejected safely.
    @Test
    fun test7_importMalformedCsv_safeRejection() = runBlocking {
        val csv = """
            Title,Amount,Category,Date
            Valid Item,12.00,Food,2026-09-01T10:00:00Z
            Bad Amount,not_a_number,Food,2026-09-01T10:00:00Z
        """.trimIndent()

        val summary = repository.importFromCsv(csv)
        assertEquals(1, summary.expensesImported)
        assertEquals(1, summary.failedRecords)
    }

    // TEST 8: Import malformed JSON. Fails without corruption.
    @Test
    fun test8_importMalformedJson_failsSafely() = runBlocking {
        val malformedJson = "{ some corrupted text without matching braces"
        val summary = repository.importFromJson(malformedJson)
        assertEquals(1, summary.failedRecords)
    }

    // TEST 9: Duplicate imports skipped.
    @Test
    fun test9_duplicateImports_skipped() = runBlocking {
        val expense = Expense(0L, "Recurring Net", BigDecimal("20.00"), "Bills", Instant.parse("2026-09-01T10:00:00Z"))
        database.expenseDao().insertExpense(expense.toEntity())

        val importPayload = FinancialBackupData(expenses = listOf(expense))
        val summary = repository.importFinancialData(importPayload, preventDuplicates = true)

        assertEquals(0, summary.expensesImported)
        assertEquals(1, summary.skippedRecords)
    }

    // TEST 10: Special characters and Unicode.
    @Test
    fun test10_specialCharactersAndUnicode() = runBlocking {
        val specialExpense = Expense(
            0L,
            "Dinner \"Special\", 🚀 with accents (Café & Crêpe)",
            BigDecimal("42.75"),
            "Food & Drink",
            Instant.parse("2026-09-01T18:00:00Z"),
            "Notes with\nnewline, commas, and quotes \"\""
        )
        database.expenseDao().insertExpense(specialExpense.toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()

        val summary = repository.restoreFromBackupBundle(backup)
        assertEquals(1, summary.expensesImported)

        val restored = repository.getFullFinancialData().expenses.first()
        assertEquals(specialExpense.title, restored.title)
        assertEquals(specialExpense.notes, restored.notes)
    }

    // TEST 11: Empty database export.
    @Test
    fun test11_emptyDatabaseExport() = runBlocking {
        val data = repository.getFullFinancialData()
        val csv = CsvExporter.exportExpensesToCsv(data.expenses)
        val json = JsonDataExporter.exportToJson(data)
        val backup = repository.createBackup()

        assertTrue(csv.startsWith("ID,Date,Title"))
        assertTrue(json.contains("\"expenses\": []"))
        assertTrue(backup.contains("FINLY_BACKUP"))
    }

    // TEST 12: Large datasets.
    @Test
    fun test12_largeDatasets() = runBlocking {
        val expenses = (1..50).map {
            Expense(0L, "Item $it", BigDecimal("10.00"), "Food", Instant.now())
        }
        database.expenseDao().insertExpenses(expenses.map { it.toEntity() })

        val backup = repository.createBackup()
        repository.clearAllData()

        val summary = repository.restoreFromBackupBundle(backup)
        assertEquals(50, summary.expensesImported)

        val restored = repository.getFullFinancialData()
        assertEquals(50, restored.expenses.size)
    }

    // TEST 13: Test backup/restore across app restart (new database instance).
    @Test
    fun test13_backupRestoreAcrossAppRestart() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Saved before restart", BigDecimal("88.00"), "Tech", Instant.now()).toEntity())
        val backup = repository.createBackup()

        // Simulate restart with a brand new database instance
        val context = ApplicationProvider.getApplicationContext<Context>()
        val newDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val newRepo = DataManagementRepositoryImpl(newDatabase)

        val summary = newRepo.restoreFromBackupBundle(backup)
        assertEquals(1, summary.expensesImported)

        val newExpenses = newRepo.getFullFinancialData().expenses
        assertEquals(1, newExpenses.size)
        assertEquals("Saved before restart", newExpenses[0].title)
        newDatabase.close()
    }

    // TEST 14: Month/year/date preservation.
    @Test
    fun test14_datePreservation() = runBlocking {
        val specificInstant = Instant.parse("2026-09-07T14:30:45Z")
        val specificMonth = YearMonth.of(2026, 9)
        val specificDate = LocalDate.of(2026, 9, 7)

        database.expenseDao().insertExpense(Expense(0L, "Dated Expense", BigDecimal("20.00"), "Misc", specificInstant).toEntity())
        database.budgetDao().insertBudget(Budget(0L, null, BigDecimal("1000.00"), specificMonth).toEntity())
        database.recurringExpenseDao().insertRecurringExpense(
            RecurringExpense(0L, "Monthly Sub", BigDecimal("12.00"), "Entertainment", RecurrenceFrequency.MONTHLY, specificDate, specificDate).toEntity()
        )

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val restored = repository.getFullFinancialData()
        assertEquals(specificInstant, restored.expenses[0].date)
        assertEquals(specificMonth, restored.budgets[0].month)
        assertEquals(specificDate, restored.recurringExpenses[0].startDate)
    }

    // TEST 15: Logical consistency across all entities after restore.
    @Test
    fun test15_logicalConsistencyAcrossAllEntities() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Groceries", BigDecimal("50.00"), "Food", Instant.now()).toEntity())
        database.incomeDao().insertIncome(Income(0L, "Salary", BigDecimal("3000.00"), "Job", Instant.now()).toEntity())
        database.budgetDao().insertBudget(Budget(0L, "Food", BigDecimal("300.00"), YearMonth.of(2026, 9)).toEntity())
        database.recurringExpenseDao().insertRecurringExpense(
            RecurringExpense(0L, "Gym", BigDecimal("40.00"), "Health", RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 1)).toEntity()
        )

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val data = repository.getFullFinancialData()
        assertEquals(1, data.expenses.size)
        assertEquals(1, data.incomes.size)
        assertEquals(1, data.budgets.size)
        assertEquals(1, data.recurringExpenses.size)
    }

    // TEST 16: Verify dashboard metrics after restore.
    @Test
    fun test16_dashboardMetricsAfterRestore() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Item 1", BigDecimal("25.00"), "Food", Instant.now()).toEntity())
        database.expenseDao().insertExpense(Expense(0L, "Item 2", BigDecimal("75.00"), "Shopping", Instant.now()).toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val expenses = database.expenseDao().getAllExpensesSync()
        val totalSpent = expenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amountInCents.toBigDecimal().movePointLeft(2) }
        assertTrue(BigDecimal("100.00").compareTo(totalSpent) == 0)
    }

    // TEST 17: Verify analytics after restore.
    @Test
    fun test17_analyticsAfterRestore() = runBlocking {
        database.expenseDao().insertExpense(Expense(0L, "Food Expense", BigDecimal("40.00"), "Food", Instant.now()).toEntity())
        database.incomeDao().insertIncome(Income(0L, "Income 1", BigDecimal("200.00"), "Job", Instant.now()).toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val balance = balanceUseCase().first()
        assertTrue(BigDecimal("160.00").compareTo(balance.netBalance) == 0)
    }

    // TEST 18: Verify budgets after restore.
    @Test
    fun test18_budgetsAfterRestore() = runBlocking {
        database.budgetDao().insertBudget(Budget(0L, "Travel", BigDecimal("1500.00"), YearMonth.of(2026, 9)).toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val budgets = database.budgetDao().getAllBudgetsSync()
        assertEquals(1, budgets.size)
        assertEquals("Travel", budgets[0].category)
        assertEquals(150000L, budgets[0].amountInCents)
    }

    // TEST 19: Verify recurring expenses after restore.
    @Test
    fun test19_recurringExpensesAfterRestore() = runBlocking {
        val rec = RecurringExpense(0L, "Cloud Storage", BigDecimal("9.99"), "Tech", RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 1))
        database.recurringExpenseDao().insertRecurringExpense(rec.toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val list = database.recurringExpenseDao().getAllRecurringExpensesSync()
        assertEquals(1, list.size)
        assertEquals("Cloud Storage", list[0].title)
    }

    // TEST 20: Verify income/balance after restore.
    @Test
    fun test20_incomeBalanceAfterRestore() = runBlocking {
        database.incomeDao().insertIncome(Income(0L, "Bonus", BigDecimal("1200.00"), "Job", Instant.now()).toEntity())
        database.expenseDao().insertExpense(Expense(0L, "Phone", BigDecimal("400.00"), "Tech", Instant.now()).toEntity())

        val backup = repository.createBackup()
        repository.clearAllData()
        repository.restoreFromBackupBundle(backup)

        val balance = balanceUseCase().first()
        assertTrue(BigDecimal("800.00").compareTo(balance.netBalance) == 0)
    }

    // TEST 21: Verify no existing Phase 1–8 functionality regressed.
    @Test
    fun test21_noPhase1To8Regression() = runBlocking {
        // Add expense
        val expId = database.expenseDao().insertExpense(Expense(0L, "Original", BigDecimal("10.00"), "Food", Instant.now()).toEntity())
        // Update expense
        database.expenseDao().updateExpense(Expense(expId, "Updated", BigDecimal("15.00"), "Food", Instant.now()).toEntity())
        // Delete expense
        database.expenseDao().deleteExpense(Expense(expId, "Updated", BigDecimal("15.00"), "Food", Instant.now()).toEntity())
        assertTrue(database.expenseDao().getAllExpensesSync().isEmpty())

        // Income CRUD
        val incId = database.incomeDao().insertIncome(Income(0L, "Salary", BigDecimal("1000.00"), "Job", Instant.now()).toEntity())
        val fetchedInc = database.incomeDao().getIncomeById(incId)
        assertNotNull(fetchedInc)
        database.incomeDao().deleteIncome(fetchedInc!!)
        assertTrue(database.incomeDao().getAllIncomesSync().isEmpty())
    }
}
