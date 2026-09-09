package com.personalexpensetracker.data.export

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class CsvImporterTest {

    @Test
    fun parseCsv_expensesExportRoundTrip_succeeds() {
        val expenses = listOf(
            Expense(
                id = 101L,
                title = "Grocery shopping",
                amount = BigDecimal("154.50"),
                category = "Food",
                date = Instant.parse("2026-09-01T12:00:00Z"),
                notes = "Organic apples & milk"
            ),
            Expense(
                id = 102L,
                title = "Bus ticket",
                amount = BigDecimal("3.25"),
                category = "Transportation",
                date = Instant.parse("2026-09-02T08:30:00Z"),
                notes = null
            )
        )

        val csv = CsvExporter.exportExpensesToCsv(expenses)
        val result = CsvImporter.parseCsv(csv)

        assertEquals(0, result.failedCount)
        assertEquals(2, result.expenses.size)
        assertEquals("Grocery shopping", result.expenses[0].title)
        assertTrue(BigDecimal("154.50").compareTo(result.expenses[0].amount) == 0)
        assertEquals("Food", result.expenses[0].category)
        assertEquals("Organic apples & milk", result.expenses[0].notes)

        assertEquals("Bus ticket", result.expenses[1].title)
        assertTrue(BigDecimal("3.25").compareTo(result.expenses[1].amount) == 0)
    }

    @Test
    fun parseCsv_incomesExportRoundTrip_succeeds() {
        val incomes = listOf(
            Income(
                id = 201L,
                title = "Monthly Salary",
                amount = BigDecimal("5500.00"),
                source = "Salary",
                date = Instant.parse("2026-09-01T09:00:00Z"),
                notes = "Direct deposit"
            )
        )

        val csv = CsvExporter.exportIncomesToCsv(incomes)
        val result = CsvImporter.parseCsv(csv)

        assertEquals(0, result.failedCount)
        assertEquals(1, result.incomes.size)
        assertEquals("Monthly Salary", result.incomes[0].title)
        assertTrue(BigDecimal("5500.00").compareTo(result.incomes[0].amount) == 0)
        assertEquals("Salary", result.incomes[0].source)
        assertEquals("Direct deposit", result.incomes[0].notes)
    }

    @Test
    fun parseCsv_combinedExportRoundTrip_succeeds() {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Dinner with, friends \"special\"",
                amount = BigDecimal("85.00"),
                category = "Food",
                date = Instant.parse("2026-09-03T19:00:00Z"),
                notes = "Line 1\nLine 2"
            )
        )
        val incomes = listOf(
            Income(
                id = 2L,
                title = "Freelance 🚀",
                amount = BigDecimal("350.75"),
                source = "Freelance",
                date = Instant.parse("2026-09-04T10:00:00Z"),
                notes = "App design"
            )
        )

        val csv = CsvExporter.exportCombinedToCsv(expenses, incomes)
        val result = CsvImporter.parseCsv(csv)

        assertEquals(0, result.failedCount)
        assertEquals(1, result.expenses.size)
        assertEquals(1, result.incomes.size)

        val parsedExpense = result.expenses[0]
        assertEquals("Dinner with, friends \"special\"", parsedExpense.title)
        assertTrue(BigDecimal("85.00").compareTo(parsedExpense.amount) == 0)
        assertEquals("Food", parsedExpense.category)
        assertEquals("Line 1\nLine 2", parsedExpense.notes)

        val parsedIncome = result.incomes[0]
        assertEquals("Freelance 🚀", parsedIncome.title)
        assertTrue(BigDecimal("350.75").compareTo(parsedIncome.amount) == 0)
        assertEquals("Freelance", parsedIncome.source)
    }

    @Test
    fun parseCsv_emptyContent_returnsError() {
        val result = CsvImporter.parseCsv("")
        assertTrue(result.failedCount > 0)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun parseCsv_missingHeaders_returnsError() {
        val csv = "Column1,Column2,Column3\r\nValue1,Value2,Value3"
        val result = CsvImporter.parseCsv(csv)
        assertTrue(result.failedCount > 0)
        assertTrue(result.errors.any { it.contains("Missing required header") })
    }

    @Test
    fun parseCsv_malformedRows_reportsFailures() {
        val csv = """
            Title,Amount,Category,Date,Notes
            Valid Item,25.50,Food,2026-09-05T12:00:00Z,None
            ,30.00,Food,2026-09-05T12:00:00Z,Missing title
            Negative Amount,-15.00,Food,2026-09-05T12:00:00Z,Bad amount
            Non Numeric,abc,Food,2026-09-05T12:00:00Z,Not a number
            Invalid Date,50.00,Food,invalid-date-string,Bad date
        """.trimIndent()

        val result = CsvImporter.parseCsv(csv)
        assertEquals(1, result.expenses.size)
        assertEquals("Valid Item", result.expenses[0].title)
        assertEquals(4, result.failedCount)
        assertEquals(4, result.errors.size)
    }

    @Test
    fun parseCsv_currencySymbolsAndDateFormats_parsedCorrectly() {
        val csv = """
            Title,Amount,Category,Date
            Item 1,"$1,250.00",Electronics,2026-09-05
            Item 2,€45.99,Shopping,2026-09-06T15:00:00Z
        """.trimIndent()

        val result = CsvImporter.parseCsv(csv)
        assertEquals(0, result.failedCount)
        assertEquals(2, result.expenses.size)
        assertTrue(BigDecimal("1250.00").compareTo(result.expenses[0].amount) == 0)
        assertTrue(BigDecimal("45.99").compareTo(result.expenses[1].amount) == 0)
    }
}
