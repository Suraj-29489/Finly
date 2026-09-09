package com.personalexpensetracker.data.export

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Robust, RFC 4180-compliant CSV generator for Expenses and Incomes (Phase 9 Step 2).
 * Handles commas, double-quotes, newlines, UTF-8 unicode, and precise decimal formatting.
 */
object CsvExporter {

    private val dateFormatter = DateTimeFormatter.ISO_INSTANT

    /**
     * Export expenses to a standard CSV string.
     */
    fun exportExpensesToCsv(expenses: List<Expense>): String {
        val writer = StringWriter()
        writer.append("ID,Date,Title,Amount,Category,Notes\r\n")

        for (expense in expenses) {
            writer.append(escape(expense.id.toString())).append(',')
            writer.append(escape(dateFormatter.format(expense.date))).append(',')
            writer.append(escape(expense.title)).append(',')
            writer.append(formatAmount(expense.amount)).append(',')
            writer.append(escape(expense.category)).append(',')
            writer.append(escape(expense.notes))
            writer.append("\r\n")
        }

        return writer.toString()
    }

    /**
     * Export incomes to a standard CSV string.
     */
    fun exportIncomesToCsv(incomes: List<Income>): String {
        val writer = StringWriter()
        writer.append("ID,Date,Title,Amount,Source,Notes\r\n")

        for (income in incomes) {
            writer.append(escape(income.id.toString())).append(',')
            writer.append(escape(dateFormatter.format(income.date))).append(',')
            writer.append(escape(income.title)).append(',')
            writer.append(formatAmount(income.amount)).append(',')
            writer.append(escape(income.source)).append(',')
            writer.append(escape(income.notes))
            writer.append("\r\n")
        }

        return writer.toString()
    }

    /**
     * Export both expenses and incomes into a single unified financial ledger CSV.
     */
    fun exportCombinedToCsv(expenses: List<Expense>, incomes: List<Income>): String {
        val writer = StringWriter()
        writer.append("Type,ID,Date,Title,Amount,CategoryOrSource,Notes\r\n")

        for (expense in expenses) {
            writer.append("EXPENSE,")
            writer.append(escape(expense.id.toString())).append(',')
            writer.append(escape(dateFormatter.format(expense.date))).append(',')
            writer.append(escape(expense.title)).append(',')
            writer.append(formatAmount(expense.amount)).append(',')
            writer.append(escape(expense.category)).append(',')
            writer.append(escape(expense.notes))
            writer.append("\r\n")
        }

        for (income in incomes) {
            writer.append("INCOME,")
            writer.append(escape(income.id.toString())).append(',')
            writer.append(escape(dateFormatter.format(income.date))).append(',')
            writer.append(escape(income.title)).append(',')
            writer.append(formatAmount(income.amount)).append(',')
            writer.append(escape(income.source)).append(',')
            writer.append(escape(income.notes))
            writer.append("\r\n")
        }

        return writer.toString()
    }

    /**
     * Escape special characters according to RFC 4180:
     * - Double quotes are escaped with two double quotes ("")
     * - Fields containing quotes, commas, or newlines are enclosed in double quotes.
     */
    fun escape(value: String?): String {
        if (value == null) return ""
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Ensure amounts are formatted with exactly 2 decimal places.
     */
    private fun formatAmount(amount: BigDecimal): String {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}

