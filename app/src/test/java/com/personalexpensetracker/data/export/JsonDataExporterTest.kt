package com.personalexpensetracker.data.export

import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class JsonDataExporterTest {

    @Test
    fun `empty dataset serializes and deserializes correctly`() {
        val emptyData = FinancialBackupData(
            version = 1,
            exportedAt = Instant.parse("2026-09-07T12:00:00Z"),
            expenses = emptyList(),
            incomes = emptyList(),
            budgets = emptyList(),
            recurringExpenses = emptyList()
        )

        val json = JsonDataExporter.exportToJson(emptyData)
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"app\": \"Finly\""))

        val reconstructed = JsonDataExporter.parseFromJson(json)
        assertEquals(1, reconstructed.version)
        assertEquals(Instant.parse("2026-09-07T12:00:00Z"), reconstructed.exportedAt)
        assertTrue(reconstructed.expenses.isEmpty())
        assertTrue(reconstructed.incomes.isEmpty())
        assertTrue(reconstructed.budgets.isEmpty())
        assertTrue(reconstructed.recurringExpenses.isEmpty())
    }

    @Test
    fun `export parse reconstruct roundtrip produces equivalent financial data`() {
        val sampleInstant = Instant.parse("2026-09-07T10:00:00Z")
        val originalData = FinancialBackupData(
            version = 1,
            exportedAt = sampleInstant,
            expenses = listOf(
                Expense(
                    id = 101L,
                    title = "Fancy Dinner 🍷",
                    amount = BigDecimal("125.75"),
                    category = "Food",
                    date = sampleInstant,
                    notes = "Birthday celebration",
                    recurringExpenseId = null,
                    createdAt = sampleInstant
                )
            ),
            incomes = listOf(
                Income(
                    id = 201L,
                    title = "Consulting Fee 💼",
                    amount = BigDecimal("4500.00"),
                    source = "Business",
                    date = sampleInstant,
                    notes = "Milestone 1 payment",
                    createdAt = sampleInstant
                )
            ),
            budgets = listOf(
                Budget(
                    id = 301L,
                    category = "Food",
                    amount = BigDecimal("600.00"),
                    month = YearMonth.of(2026, 9),
                    createdAt = sampleInstant
                ),
                Budget(
                    id = 302L,
                    category = null, // Overall budget
                    amount = BigDecimal("2500.00"),
                    month = YearMonth.of(2026, 9),
                    createdAt = sampleInstant
                )
            ),
            recurringExpenses = listOf(
                RecurringExpense(
                    id = 401L,
                    title = "Streaming Pass",
                    amount = BigDecimal("14.99"),
                    category = "Entertainment",
                    frequency = RecurrenceFrequency.MONTHLY,
                    startDate = LocalDate.of(2026, 1, 1),
                    nextOccurrenceDate = LocalDate.of(2026, 10, 1),
                    lastGeneratedDate = LocalDate.of(2026, 9, 1),
                    notes = "Family plan",
                    isActive = true,
                    createdAt = sampleInstant
                )
            )
        )

        val json = JsonDataExporter.exportToJson(originalData)
        val parsed = JsonDataExporter.parseFromJson(json)

        // Verify Expense
        assertEquals(1, parsed.expenses.size)
        val expense = parsed.expenses.first()
        assertEquals(101L, expense.id)
        assertEquals("Fancy Dinner 🍷", expense.title)
        assertEquals(BigDecimal("125.75"), expense.amount)
        assertEquals("Food", expense.category)
        assertEquals(sampleInstant, expense.date)
        assertEquals("Birthday celebration", expense.notes)
        assertNull(expense.recurringExpenseId)

        // Verify Income
        assertEquals(1, parsed.incomes.size)
        val income = parsed.incomes.first()
        assertEquals(201L, income.id)
        assertEquals("Consulting Fee 💼", income.title)
        assertEquals(BigDecimal("4500.00"), income.amount)
        assertEquals("Business", income.source)
        assertEquals(sampleInstant, income.date)
        assertEquals("Milestone 1 payment", income.notes)

        // Verify Budgets (Category & Overall)
        assertEquals(2, parsed.budgets.size)
        val catBudget = parsed.budgets.find { it.category == "Food" }!!
        assertEquals(BigDecimal("600.00"), catBudget.amount)
        val overallBudget = parsed.budgets.find { it.category == null }!!
        assertEquals(BigDecimal("2500.00"), overallBudget.amount)

        // Verify Recurring
        assertEquals(1, parsed.recurringExpenses.size)
        val rec = parsed.recurringExpenses.first()
        assertEquals("Streaming Pass", rec.title)
        assertEquals(BigDecimal("14.99"), rec.amount)
        assertEquals(RecurrenceFrequency.MONTHLY, rec.frequency)
        assertEquals(LocalDate.of(2026, 10, 1), rec.nextOccurrenceDate)
        assertEquals(LocalDate.of(2026, 9, 1), rec.lastGeneratedDate)
        assertTrue(rec.isActive)
    }

    @Test
    fun `handles missing optional fields safely`() {
        val minimalJson = """
        {
            "version": 1,
            "expenses": [
                {
                    "title": "Quick Cash Snack",
                    "amount": "3.50",
                    "category": "Food",
                    "date": "2026-09-07T14:00:00Z"
                }
            ],
            "incomes": [
                {
                    "title": "Gift",
                    "amount": "50.00",
                    "source": "Gift",
                    "date": "2026-09-07T14:00:00Z"
                }
            ]
        }
        """.trimIndent()

        val parsed = JsonDataExporter.parseFromJson(minimalJson)
        assertEquals(1, parsed.expenses.size)
        assertNull(parsed.expenses.first().notes)
        assertNull(parsed.expenses.first().recurringExpenseId)

        assertEquals(1, parsed.incomes.size)
        assertNull(parsed.incomes.first().notes)
    }

    @Test
    fun `parseFromJsonSafely handles malformed and empty JSON gracefully`() {
        val emptyResult = JsonDataExporter.parseFromJsonSafely("")
        assertEquals(1, emptyResult.failedCount)
        assertTrue(emptyResult.errors.first().contains("empty"))

        val malformedResult = JsonDataExporter.parseFromJsonSafely("{ not valid json : 123 }")
        assertEquals(1, malformedResult.failedCount)
        assertTrue(malformedResult.errors.first().contains("Malformed JSON"))
    }

    @Test
    fun `parseFromJsonSafely rejects unsupported future schema versions`() {
        val futureJson = """
        {
            "version": 999,
            "expenses": []
        }
        """.trimIndent()

        val result = JsonDataExporter.parseFromJsonSafely(futureJson)
        assertEquals(1, result.failedCount)
        assertTrue(result.errors.first().contains("Unsupported backup schema version"))
    }

    @Test
    fun `parseFromJsonSafely detects invalid records`() {
        val invalidJson = """
        {
            "version": 1,
            "expenses": [
                {
                    "title": "",
                    "amount": "-5.00",
                    "category": "Food",
                    "date": "2026-09-07T14:00:00Z"
                }
            ]
        }
        """.trimIndent()

        val result = JsonDataExporter.parseFromJsonSafely(invalidJson)
        assertEquals(1, result.failedCount)
        assertTrue(result.errors.first().contains("Expense invalid"))
    }
}

