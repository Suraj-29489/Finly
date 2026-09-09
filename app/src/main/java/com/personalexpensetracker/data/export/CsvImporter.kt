package com.personalexpensetracker.data.export

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Result of parsing a CSV file.
 */
data class CsvParseResult(
    val expenses: List<Expense> = emptyList(),
    val incomes: List<Income> = emptyList(),
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * RFC 4180 compliant CSV parser and validator for Expenses and Incomes (Phase 9 Step 4).
 * Handles multiline fields, escaped quotes, various date formats, currency symbols, and detailed validation errors.
 */
object CsvImporter {

    /**
     * Parse raw CSV content into typed Expense and Income domain models.
     */
    fun parseCsv(csvContent: String): CsvParseResult {
        if (csvContent.isBlank()) {
            return CsvParseResult(
                failedCount = 1,
                errors = listOf("CSV file is empty")
            )
        }

        val rows = parseCsvRows(csvContent)
        if (rows.isEmpty()) {
            return CsvParseResult(
                failedCount = 1,
                errors = listOf("No records found in CSV")
            )
        }

        val headerRow = rows.first()
        val headerMap = headerRow.mapIndexed { index, name ->
            name.trim().lowercase() to index
        }.toMap()

        // Determine CSV format based on headers
        val hasType = headerMap.containsKey("type")
        val hasSource = headerMap.containsKey("source")
        val hasCategory = headerMap.containsKey("category") || headerMap.containsKey("categoryorsource")

        val titleIndex = headerMap["title"] ?: headerMap["description"] ?: headerMap["name"]
        val amountIndex = headerMap["amount"]
        val dateIndex = headerMap["date"] ?: headerMap["timestamp"]
        val notesIndex = headerMap["notes"] ?: headerMap["note"]
        val typeIndex = headerMap["type"]
        val categoryIndex = headerMap["category"] ?: headerMap["categoryorsource"] ?: headerMap["source"]

        if (titleIndex == null || amountIndex == null || dateIndex == null) {
            return CsvParseResult(
                failedCount = 1,
                errors = listOf("Missing required header columns (Title, Amount, Date required). Found headers: ${headerRow.joinToString(", ")}")
            )
        }

        val parsedExpenses = mutableListOf<Expense>()
        val parsedIncomes = mutableListOf<Income>()
        val errors = mutableListOf<String>()
        var failedCount = 0

        for (rowIndex in 1 until rows.size) {
            val row = rows[rowIndex]
            val lineNumber = rowIndex + 1

            if (row.all { it.isBlank() }) {
                // Skip empty blank row
                continue
            }

            // Extract fields safely
            val title = row.getOrNull(titleIndex)?.trim().orEmpty()
            if (title.isBlank()) {
                failedCount++
                errors.add("Line $lineNumber: Title cannot be empty")
                continue
            }

            val rawAmount = row.getOrNull(amountIndex)?.trim().orEmpty()
            val amount = parseAmount(rawAmount)
            if (amount == null || amount <= BigDecimal.ZERO) {
                failedCount++
                errors.add("Line $lineNumber: Invalid or non-positive amount '$rawAmount'")
                continue
            }

            val rawDate = row.getOrNull(dateIndex)?.trim().orEmpty()
            val date = parseDate(rawDate)
            if (date == null) {
                failedCount++
                errors.add("Line $lineNumber: Invalid date format '$rawDate'")
                continue
            }

            val notes = notesIndex?.let { row.getOrNull(it)?.trim() }?.ifBlank { null }
            val rawCategory = categoryIndex?.let { row.getOrNull(it)?.trim() }?.ifBlank { "Other" } ?: "Other"

            val recordType = if (hasType && typeIndex != null) {
                row.getOrNull(typeIndex)?.trim()?.uppercase() ?: "EXPENSE"
            } else if (hasSource && !hasCategory) {
                "INCOME"
            } else {
                "EXPENSE"
            }

            if (recordType == "INCOME") {
                parsedIncomes.add(
                    Income(
                        id = 0L,
                        title = title,
                        amount = amount,
                        source = rawCategory,
                        date = date,
                        notes = notes,
                        createdAt = Instant.now()
                    )
                )
            } else {
                parsedExpenses.add(
                    Expense(
                        id = 0L,
                        title = title,
                        amount = amount,
                        category = rawCategory,
                        date = date,
                        notes = notes,
                        createdAt = Instant.now()
                    )
                )
            }
        }

        return CsvParseResult(
            expenses = parsedExpenses,
            incomes = parsedIncomes,
            skippedCount = 0,
            failedCount = failedCount,
            errors = errors
        )
    }

    /**
     * Parses RFC 4180 CSV content into rows and fields.
     */
    fun parseCsvRows(csvContent: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()

        var inQuotes = false
        var i = 0
        val len = csvContent.length

        while (i < len) {
            val c = csvContent[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvContent[i + 1] == '"') {
                        // Escaped quote ("")
                        currentField.append('"')
                        i++ // Skip second quote
                    } else {
                        // Closing quote
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        // Check for CRLF
                        if (i + 1 < len && csvContent[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        rows.add(ArrayList(currentRow))
                        currentRow.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return rows
    }

    private fun parseAmount(raw: String): BigDecimal? {
        if (raw.isBlank()) return null
        val cleaned = raw.replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .replace("₹", "")
            .replace(",", "")
            .trim()
        return try {
            BigDecimal(cleaned)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDate(raw: String): Instant? {
        if (raw.isBlank()) return null

        // 1. Try ISO_INSTANT (e.g. 2026-09-08T10:15:30Z)
        try {
            return Instant.parse(raw)
        } catch (_: DateTimeParseException) {}

        // 2. Try LocalDate (e.g. 2026-09-08)
        try {
            val localDate = LocalDate.parse(raw)
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        } catch (_: DateTimeParseException) {}

        // 3. Try epoch millis
        try {
            val millis = raw.toLong()
            return Instant.ofEpochMilli(millis)
        } catch (_: NumberFormatException) {}

        return null
    }
}

