package com.personalexpensetracker.data.backup

import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class BackupManagerTest {

    @Test
    fun createAndReadBackupBundle_roundTripPreservesAllFinancialData() {
        val originalData = FinancialBackupData(
            version = 1,
            exportedAt = Instant.parse("2026-09-07T12:00:00Z"),
            expenses = listOf(
                Expense(
                    id = 1L,
                    title = "Dinner with friends",
                    amount = BigDecimal("85.50"),
                    category = "Food",
                    date = Instant.parse("2026-09-05T19:30:00Z")
                )
            ),
            incomes = listOf(
                Income(
                    id = 2L,
                    title = "Design Contract",
                    amount = BigDecimal("1200.00"),
                    source = "Freelance",
                    date = Instant.parse("2026-09-01T10:00:00Z")
                )
            ),
            budgets = listOf(
                Budget(
                    id = 3L,
                    category = "Food",
                    amount = BigDecimal("400.00"),
                    month = YearMonth.of(2026, 9)
                )
            ),
            recurringExpenses = listOf(
                RecurringExpense(
                    id = 4L,
                    title = "Gym Membership",
                    amount = BigDecimal("49.99"),
                    category = "Health",
                    frequency = RecurrenceFrequency.MONTHLY,
                    startDate = LocalDate.of(2026, 1, 1),
                    nextOccurrenceDate = LocalDate.of(2026, 10, 1)
                )
            )
        )

        val bundle = BackupManager.createBackupBundle(originalData)
        val result = BackupManager.readBackupBundle(bundle)

        assertTrue(result is BackupReadResult.Success)
        val success = result as BackupReadResult.Success
        assertEquals(1, success.data.expenses.size)
        assertEquals("Dinner with friends", success.data.expenses[0].title)
        assertTrue(BigDecimal("85.50").compareTo(success.data.expenses[0].amount) == 0)

        assertEquals(1, success.data.incomes.size)
        assertEquals("Design Contract", success.data.incomes[0].title)

        assertEquals(1, success.data.budgets.size)
        assertEquals("Food", success.data.budgets[0].category)

        assertEquals(1, success.data.recurringExpenses.size)
        assertEquals("Gym Membership", success.data.recurringExpenses[0].title)
    }

    @Test
    fun createAndReadBackupBundle_emptyDatasetSucceeds() {
        val emptyData = FinancialBackupData()
        val bundle = BackupManager.createBackupBundle(emptyData)
        val result = BackupManager.readBackupBundle(bundle)

        assertTrue(result is BackupReadResult.Success)
        val success = result as BackupReadResult.Success
        assertTrue(success.data.expenses.isEmpty())
        assertTrue(success.data.incomes.isEmpty())
        assertTrue(success.data.budgets.isEmpty())
        assertTrue(success.data.recurringExpenses.isEmpty())
    }

    @Test
    fun readBackupBundle_detectsTamperingAndFailsChecksum() {
        val originalData = FinancialBackupData(
            expenses = listOf(Expense(1L, "Legit Expense", BigDecimal("10.00"), "Misc", Instant.now()))
        )
        val bundle = BackupManager.createBackupBundle(originalData)

        // Tamper with payload
        val tamperedBundle = bundle.replace("10.00", "9999.00")
        val result = BackupManager.readBackupBundle(tamperedBundle)

        assertTrue(result is BackupReadResult.Failure)
        val failure = result as BackupReadResult.Failure
        assertTrue(failure.message.contains("integrity check failed"))
    }

    @Test
    fun readBackupBundle_invalidMagicHeader_fails() {
        val invalidBundle = """
        {
            "magic": "NOT_FINLY",
            "backupVersion": 1,
            "checksum": "abc",
            "payload": "{}"
        }
        """.trimIndent()

        val result = BackupManager.readBackupBundle(invalidBundle)
        assertTrue(result is BackupReadResult.Failure)
        val failure = result as BackupReadResult.Failure
        assertTrue(failure.message.contains("Not a valid Finly backup file"))
    }

    @Test
    fun readBackupBundle_unsupportedVersion_fails() {
        val futureBundle = """
        {
            "magic": "FINLY_BACKUP",
            "backupVersion": 99,
            "checksum": "abc",
            "payload": "{}"
        }
        """.trimIndent()

        val result = BackupManager.readBackupBundle(futureBundle)
        assertTrue(result is BackupReadResult.Failure)
        val failure = result as BackupReadResult.Failure
        assertTrue(failure.message.contains("Unsupported backup version"))
    }

    @Test
    fun generateDefaultBackupFileName_hasCorrectExtension() {
        val filename = BackupManager.generateDefaultBackupFileName()
        assertTrue(filename.startsWith("finly_backup_"))
        assertTrue(filename.endsWith(BackupManager.BACKUP_FILE_EXTENSION))
    }
}

