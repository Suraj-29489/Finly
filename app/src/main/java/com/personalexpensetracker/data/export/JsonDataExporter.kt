package com.personalexpensetracker.data.export

import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Structured, versioned JSON serialization and deserialization for Finly financial data (Phase 9 Step 3).
 * Robustly handles Unicode, decimals, dates, optional values, and round-trip verification.
 */
object JsonDataExporter {

    private const val CURRENT_FORMAT_VERSION = 1
    private const val APP_NAME = "Finly"
    private val instantFormatter = DateTimeFormatter.ISO_INSTANT
    private val localDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Serialize a [FinancialBackupData] snapshot into a formatted JSON string.
     */
    fun exportToJson(data: FinancialBackupData, indentSpaces: Int = 2): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("app", APP_NAME)
        root.put("exportedAt", instantFormatter.format(data.exportedAt))

        // 1. Expenses
        val expensesArray = JSONArray()
        for (expense in data.expenses) {
            val obj = JSONObject()
            obj.put("id", expense.id)
            obj.put("title", expense.title)
            obj.put("amount", formatAmount(expense.amount))
            obj.put("category", expense.category)
            obj.put("date", instantFormatter.format(expense.date))
            if (expense.notes != null) obj.put("notes", expense.notes)
            if (expense.recurringExpenseId != null) obj.put("recurringExpenseId", expense.recurringExpenseId)
            obj.put("createdAt", instantFormatter.format(expense.createdAt))
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // 2. Incomes
        val incomesArray = JSONArray()
        for (income in data.incomes) {
            val obj = JSONObject()
            obj.put("id", income.id)
            obj.put("title", income.title)
            obj.put("amount", formatAmount(income.amount))
            obj.put("source", income.source)
            obj.put("date", instantFormatter.format(income.date))
            if (income.notes != null) obj.put("notes", income.notes)
            obj.put("createdAt", instantFormatter.format(income.createdAt))
            incomesArray.put(obj)
        }
        root.put("incomes", incomesArray)

        // 3. Budgets
        val budgetsArray = JSONArray()
        for (budget in data.budgets) {
            val obj = JSONObject()
            obj.put("id", budget.id)
            obj.put("month", budget.month.toString())
            if (budget.category != null) obj.put("category", budget.category)
            obj.put("amount", formatAmount(budget.amount))
            obj.put("createdAt", instantFormatter.format(budget.createdAt))
            budgetsArray.put(obj)
        }
        root.put("budgets", budgetsArray)

        // 4. Recurring Expenses
        val recurringArray = JSONArray()
        for (rec in data.recurringExpenses) {
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("title", rec.title)
            obj.put("amount", formatAmount(rec.amount))
            obj.put("category", rec.category)
            obj.put("frequency", rec.frequency.name)
            obj.put("startDate", localDateFormatter.format(rec.startDate))
            obj.put("nextOccurrenceDate", localDateFormatter.format(rec.nextOccurrenceDate))
            if (rec.lastGeneratedDate != null) obj.put("lastGeneratedDate", localDateFormatter.format(rec.lastGeneratedDate))
            if (rec.notes != null) obj.put("notes", rec.notes)
            obj.put("isActive", rec.isActive)
            obj.put("createdAt", instantFormatter.format(rec.createdAt))
            recurringArray.put(obj)
        }
        root.put("recurringExpenses", recurringArray)

        return if (indentSpaces > 0) root.toString(indentSpaces) else root.toString()
    }

    /**
     * Parse and deserialize JSON string into a validated [FinancialBackupData] object.
     */
    fun parseFromJson(jsonString: String): FinancialBackupData {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", CURRENT_FORMAT_VERSION)
        val exportedAt = if (root.has("exportedAt")) {
            Instant.parse(root.getString("exportedAt"))
        } else {
            Instant.now()
        }

        // Expenses
        val expenses = mutableListOf<Expense>()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            expenses.add(
                Expense(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    amount = BigDecimal(obj.getString("amount")),
                    category = obj.getString("category"),
                    date = Instant.parse(obj.getString("date")),
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                    recurringExpenseId = if (obj.has("recurringExpenseId") && !obj.isNull("recurringExpenseId")) obj.getLong("recurringExpenseId") else null,
                    createdAt = if (obj.has("createdAt")) Instant.parse(obj.getString("createdAt")) else Instant.now()
                )
            )
        }

        // Incomes
        val incomes = mutableListOf<Income>()
        val incomesArray = root.optJSONArray("incomes") ?: JSONArray()
        for (i in 0 until incomesArray.length()) {
            val obj = incomesArray.getJSONObject(i)
            incomes.add(
                Income(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    amount = BigDecimal(obj.getString("amount")),
                    source = obj.getString("source"),
                    date = Instant.parse(obj.getString("date")),
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                    createdAt = if (obj.has("createdAt")) Instant.parse(obj.getString("createdAt")) else Instant.now()
                )
            )
        }

        // Budgets
        val budgets = mutableListOf<Budget>()
        val budgetsArray = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until budgetsArray.length()) {
            val obj = budgetsArray.getJSONObject(i)
            budgets.add(
                Budget(
                    id = obj.optLong("id", 0L),
                    category = if (obj.has("category") && !obj.isNull("category")) obj.getString("category") else null,
                    amount = BigDecimal(obj.getString("amount")),
                    month = YearMonth.parse(obj.getString("month")),
                    createdAt = if (obj.has("createdAt")) Instant.parse(obj.getString("createdAt")) else Instant.now()
                )
            )
        }

        // Recurring Expenses
        val recurringExpenses = mutableListOf<RecurringExpense>()
        val recurringArray = root.optJSONArray("recurringExpenses") ?: JSONArray()
        for (i in 0 until recurringArray.length()) {
            val obj = recurringArray.getJSONObject(i)
            recurringExpenses.add(
                RecurringExpense(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    amount = BigDecimal(obj.getString("amount")),
                    category = obj.getString("category"),
                    frequency = RecurrenceFrequency.valueOf(obj.getString("frequency")),
                    startDate = LocalDate.parse(obj.getString("startDate")),
                    nextOccurrenceDate = LocalDate.parse(obj.getString("nextOccurrenceDate")),
                    lastGeneratedDate = if (obj.has("lastGeneratedDate") && !obj.isNull("lastGeneratedDate")) LocalDate.parse(obj.getString("lastGeneratedDate")) else null,
                    notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = if (obj.has("createdAt")) Instant.parse(obj.getString("createdAt")) else Instant.now()
                )
            )
        }

        return FinancialBackupData(
            version = version,
            exportedAt = exportedAt,
            expenses = expenses,
            incomes = incomes,
            budgets = budgets,
            recurringExpenses = recurringExpenses
        )
    }

    /**
     * Safely parse and validate JSON content, returning error descriptions instead of throwing exceptions.
     */
    fun parseFromJsonSafely(jsonString: String): JsonParseResult {
        if (jsonString.isBlank()) {
            return JsonParseResult(failedCount = 1, errors = listOf("JSON file is empty"))
        }

        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            return JsonParseResult(failedCount = 1, errors = listOf("Malformed JSON: ${e.localizedMessage}"))
        }

        val version = root.optInt("version", CURRENT_FORMAT_VERSION)
        if (version > CURRENT_FORMAT_VERSION) {
            return JsonParseResult(
                failedCount = 1,
                errors = listOf("Unsupported backup schema version: $version (max supported is $CURRENT_FORMAT_VERSION)")
            )
        }

        if (!root.has("expenses") && !root.has("incomes") && !root.has("budgets") && !root.has("recurringExpenses")) {
            return JsonParseResult(
                failedCount = 1,
                errors = listOf("JSON does not contain any recognizable financial collections (expenses, incomes, budgets, recurringExpenses)")
            )
        }

        return try {
            val data = parseFromJson(jsonString)
            // Validate records
            val errors = mutableListOf<String>()
            var failedCount = 0

            for (expense in data.expenses) {
                if (expense.title.isBlank() || expense.amount <= BigDecimal.ZERO) {
                    failedCount++
                    errors.add("Expense invalid: '${expense.title}' with amount ${expense.amount}")
                }
            }
            for (income in data.incomes) {
                if (income.title.isBlank() || income.amount <= BigDecimal.ZERO) {
                    failedCount++
                    errors.add("Income invalid: '${income.title}' with amount ${income.amount}")
                }
            }

            if (failedCount > 0) {
                JsonParseResult(data = null, failedCount = failedCount, errors = errors)
            } else {
                JsonParseResult(data = data, failedCount = 0, errors = emptyList())
            }
        } catch (e: Exception) {
            JsonParseResult(failedCount = 1, errors = listOf("Failed to parse financial data from JSON: ${e.localizedMessage}"))
        }
    }

    private fun formatAmount(amount: BigDecimal): String {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}

/**
 * Result of safe JSON parsing.
 */
data class JsonParseResult(
    val data: FinancialBackupData? = null,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList()
)

