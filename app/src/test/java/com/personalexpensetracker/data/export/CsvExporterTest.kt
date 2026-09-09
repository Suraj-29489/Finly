package com.personalexpensetracker.data.export

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class CsvExporterTest {

    @Test
    fun `empty dataset produces header only`() {
        val expenseCsv = CsvExporter.exportExpensesToCsv(emptyList())
        assertEquals("ID,Date,Title,Amount,Category,Notes\r\n", expenseCsv)

        val incomeCsv = CsvExporter.exportIncomesToCsv(emptyList())
        assertEquals("ID,Date,Title,Amount,Source,Notes\r\n", incomeCsv)

        val combinedCsv = CsvExporter.exportCombinedToCsv(emptyList(), emptyList())
        assertEquals("Type,ID,Date,Title,Amount,CategoryOrSource,Notes\r\n", combinedCsv)
    }

    @Test
    fun `normal export produces correct headers and formatted values`() {
        val expenses = listOf(
            Expense(
                id = 1L,
                title = "Coffee",
                amount = BigDecimal("4.50"),
                category = "Food",
                date = Instant.parse("2026-09-01T10:00:00Z"),
                notes = "Morning cappuccino"
            )
        )

        val csv = CsvExporter.exportExpensesToCsv(expenses)
        val lines = csv.split("\r\n")

        assertEquals("ID,Date,Title,Amount,Category,Notes", lines[0])
        assertEquals("1,2026-09-01T10:00:00Z,Coffee,4.50,Food,Morning cappuccino", lines[1])
    }

    @Test
    fun `special characters commas quotes and newlines are properly escaped`() {
        val expenses = listOf(
            Expense(
                id = 2L,
                title = "Books, pens, and \"notebooks\"",
                amount = BigDecimal("45.99"),
                category = "Education",
                date = Instant.parse("2026-09-02T12:00:00Z"),
                notes = "Line 1\nLine 2 with, comma"
            )
        )

        val csv = CsvExporter.exportExpensesToCsv(expenses)
        assertTrue(csv.contains("\"Books, pens, and \"\"notebooks\"\"\""))
        assertTrue(csv.contains("\"Line 1\nLine 2 with, comma\""))
    }

    @Test
    fun `unicode text and emojis are preserved intact`() {
        val incomes = listOf(
            Income(
                id = 10L,
                title = "Freelance デザイン 🎉",
                amount = BigDecimal("1500.00"),
                source = "Freelance 💼",
                date = Instant.parse("2026-09-03T15:30:00Z"),
                notes = "Café & Théâtre payment ☕"
            )
        )

        val csv = CsvExporter.exportIncomesToCsv(incomes)
        assertTrue(csv.contains("Freelance デザイン 🎉"))
        assertTrue(csv.contains("Freelance 💼"))
        assertTrue(csv.contains("Café & Théâtre payment ☕"))
    }

    @Test
    fun `multiple records and combined ledger format are accurate`() {
        val expenses = listOf(
            Expense(id = 1L, title = "Lunch", amount = BigDecimal("15.00"), category = "Food", date = Instant.parse("2026-09-01T12:00:00Z")),
            Expense(id = 2L, title = "Dinner", amount = BigDecimal("30.00"), category = "Food", date = Instant.parse("2026-09-01T19:00:00Z"))
        )
        val incomes = listOf(
            Income(id = 1L, title = "Bonus", amount = BigDecimal("200.00"), source = "Salary", date = Instant.parse("2026-09-01T09:00:00Z"))
        )

        val combined = CsvExporter.exportCombinedToCsv(expenses, incomes)
        val lines = combined.trim().split("\r\n")

        assertEquals(4, lines.size) // Header + 2 expenses + 1 income
        assertEquals("Type,ID,Date,Title,Amount,CategoryOrSource,Notes", lines[0])
        assertTrue(lines[1].startsWith("EXPENSE,1,"))
        assertTrue(lines[2].startsWith("EXPENSE,2,"))
        assertTrue(lines[3].startsWith("INCOME,1,"))
    }
}

