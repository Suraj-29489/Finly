package com.personalexpensetracker.data.repository

import androidx.room.withTransaction
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.database.BudgetEntity
import com.personalexpensetracker.data.database.ExpenseEntity
import com.personalexpensetracker.data.database.IncomeEntity
import com.personalexpensetracker.data.database.RecurringExpenseEntity
import com.personalexpensetracker.data.backup.BackupCryptoManager
import com.personalexpensetracker.data.backup.BackupManager
import com.personalexpensetracker.data.backup.BackupReadResult
import com.personalexpensetracker.data.backup.CryptoResult
import com.personalexpensetracker.data.export.CsvImporter
import com.personalexpensetracker.data.export.JsonDataExporter
import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.datamanagement.ImportSummary
import com.personalexpensetracker.domain.repository.DataManagementRepository
import java.time.Instant

/**
 * Concrete implementation of DataManagementRepository with atomic transactions (Phase 9 Step 1).
 */
class DataManagementRepositoryImpl(
    private val database: AppDatabase
) : DataManagementRepository {

    private val expenseDao = database.expenseDao()
    private val incomeDao = database.incomeDao()
    private val budgetDao = database.budgetDao()
    private val recurringExpenseDao = database.recurringExpenseDao()

    override suspend fun getFullFinancialData(): FinancialBackupData {
        val expenses = expenseDao.getAllExpensesSync().map { it.toDomain() }
        val incomes = incomeDao.getAllIncomesSync().map { it.toDomain() }
        val budgets = budgetDao.getAllBudgetsSync().map { it.toDomain() }
        val recurring = recurringExpenseDao.getAllRecurringExpensesSync().map { it.toDomain() }

        return FinancialBackupData(
            version = 1,
            exportedAt = Instant.now(),
            expenses = expenses,
            incomes = incomes,
            budgets = budgets,
            recurringExpenses = recurring
        )
    }

    override suspend fun restoreFinancialData(data: FinancialBackupData): ImportSummary {
        return database.withTransaction {
            // 1. Wipe current tables atomically
            expenseDao.deleteAllExpenses()
            incomeDao.deleteAllIncomes()
            budgetDao.deleteAllBudgets()
            recurringExpenseDao.deleteAllRecurringExpenses()

            // 2. Insert records from backup
            val expenseEntities = data.expenses.map { it.toEntity().copy(id = 0L) }
            val insertedExpenses = expenseDao.insertExpenses(expenseEntities)

            val incomeEntities = data.incomes.map { it.toEntity().copy(id = 0L) }
            val insertedIncomes = incomeDao.insertIncomes(incomeEntities)

            val budgetEntities = data.budgets.map { it.toEntity().copy(id = 0L) }
            val insertedBudgets = budgetDao.insertBudgets(budgetEntities)

            val recurringEntities = data.recurringExpenses.map { it.toEntity().copy(id = 0L) }
            val insertedRecurring = recurringExpenseDao.insertRecurringExpenses(recurringEntities)

            ImportSummary(
                expensesImported = insertedExpenses.size,
                incomesImported = insertedIncomes.size,
                budgetsImported = insertedBudgets.size,
                recurringExpensesImported = insertedRecurring.size,
                skippedRecords = 0,
                failedRecords = 0
            )
        }
    }

    override suspend fun importFinancialData(
        data: FinancialBackupData,
        preventDuplicates: Boolean
    ): ImportSummary {
        return database.withTransaction {
            val existingExpenses = expenseDao.getAllExpensesSync()
            val existingIncomes = incomeDao.getAllIncomesSync()
            val existingBudgets = budgetDao.getAllBudgetsSync()
            val existingRecurring = recurringExpenseDao.getAllRecurringExpensesSync()

            var expensesCount = 0
            var incomesCount = 0
            var budgetsCount = 0
            var recurringCount = 0
            var skippedCount = 0
            val errors = mutableListOf<String>()

            // 1. Expenses
            val expensesToInsert = mutableListOf<ExpenseEntity>()
            for (expense in data.expenses) {
                val entity = expense.toEntity().copy(id = 0L)
                if (preventDuplicates && existingExpenses.any {
                    it.title.equals(entity.title, ignoreCase = true) &&
                    it.amountInCents == entity.amountInCents &&
                    it.date == entity.date
                }) {
                    skippedCount++
                } else {
                    expensesToInsert.add(entity)
                    expensesCount++
                }
            }
            if (expensesToInsert.isNotEmpty()) {
                expenseDao.insertExpenses(expensesToInsert)
            }

            // 2. Incomes
            val incomesToInsert = mutableListOf<IncomeEntity>()
            for (income in data.incomes) {
                val entity = income.toEntity().copy(id = 0L)
                if (preventDuplicates && existingIncomes.any {
                    it.title.equals(entity.title, ignoreCase = true) &&
                    it.amountInCents == entity.amountInCents &&
                    it.date == entity.date
                }) {
                    skippedCount++
                } else {
                    incomesToInsert.add(entity)
                    incomesCount++
                }
            }
            if (incomesToInsert.isNotEmpty()) {
                incomeDao.insertIncomes(incomesToInsert)
            }

            // 3. Budgets
            val budgetsToInsert = mutableListOf<BudgetEntity>()
            for (budget in data.budgets) {
                val entity = budget.toEntity().copy(id = 0L)
                if (preventDuplicates && existingBudgets.any {
                    it.month == entity.month && it.category == entity.category
                }) {
                    skippedCount++
                } else {
                    budgetsToInsert.add(entity)
                    budgetsCount++
                }
            }
            if (budgetsToInsert.isNotEmpty()) {
                budgetDao.insertBudgets(budgetsToInsert)
            }

            // 4. Recurring
            val recurringToInsert = mutableListOf<RecurringExpenseEntity>()
            for (recurring in data.recurringExpenses) {
                val entity = recurring.toEntity().copy(id = 0L)
                if (preventDuplicates && existingRecurring.any {
                    it.title.equals(entity.title, ignoreCase = true) &&
                    it.amountInCents == entity.amountInCents &&
                    it.frequency == entity.frequency
                }) {
                    skippedCount++
                } else {
                    recurringToInsert.add(entity)
                    recurringCount++
                }
            }
            if (recurringToInsert.isNotEmpty()) {
                recurringExpenseDao.insertRecurringExpenses(recurringToInsert)
            }

            ImportSummary(
                expensesImported = expensesCount,
                incomesImported = incomesCount,
                budgetsImported = budgetsCount,
                recurringExpensesImported = recurringCount,
                skippedRecords = skippedCount,
                failedRecords = 0,
                errors = errors
            )
        }
    }

    override suspend fun clearAllData() {
        database.withTransaction {
            expenseDao.deleteAllExpenses()
            incomeDao.deleteAllIncomes()
            budgetDao.deleteAllBudgets()
            recurringExpenseDao.deleteAllRecurringExpenses()
        }
    }

    override suspend fun importFromCsv(csvContent: String, preventDuplicates: Boolean): ImportSummary {
        val parseResult = CsvImporter.parseCsv(csvContent)
        if (parseResult.failedCount > 0 && parseResult.expenses.isEmpty() && parseResult.incomes.isEmpty()) {
            return ImportSummary(
                failedRecords = parseResult.failedCount,
                errors = parseResult.errors
            )
        }

        val partialSummary = importFinancialData(
            FinancialBackupData(
                expenses = parseResult.expenses,
                incomes = parseResult.incomes
            ),
            preventDuplicates = preventDuplicates
        )

        return partialSummary.copy(
            failedRecords = partialSummary.failedRecords + parseResult.failedCount,
            errors = partialSummary.errors + parseResult.errors
        )
    }

    override suspend fun importFromJson(jsonContent: String, preventDuplicates: Boolean): ImportSummary {
        val parseResult = JsonDataExporter.parseFromJsonSafely(jsonContent)
        if (parseResult.data == null) {
            return ImportSummary(
                failedRecords = parseResult.failedCount,
                errors = parseResult.errors
            )
        }

        val partialSummary = importFinancialData(
            parseResult.data,
            preventDuplicates = preventDuplicates
        )

        return partialSummary.copy(
            failedRecords = partialSummary.failedRecords + parseResult.failedCount,
            errors = partialSummary.errors + parseResult.errors
        )
    }

    override suspend fun createBackup(password: CharArray?): String {
        val fullData = getFullFinancialData()
        val plainBundle = BackupManager.createBackupBundle(fullData)

        if (password != null && password.isNotEmpty()) {
            when (val encryptResult = BackupCryptoManager.encryptBackup(plainBundle, password)) {
                is CryptoResult.Success -> return encryptResult.data
                is CryptoResult.Failure -> throw IllegalStateException("Failed to encrypt backup: ${encryptResult.message}", encryptResult.cause)
            }
        }

        return plainBundle
    }

    override suspend fun restoreFromBackupBundle(
        bundleContent: String,
        password: CharArray?
    ): ImportSummary {
        val plainBundleString = if (BackupCryptoManager.isEncryptedBackup(bundleContent)) {
            if (password == null || password.isEmpty()) {
                return ImportSummary(
                    failedRecords = 1,
                    errors = listOf("Password required to decrypt this backup")
                )
            }
            when (val decryptResult = BackupCryptoManager.decryptBackup(bundleContent, password)) {
                is CryptoResult.Success -> decryptResult.data
                is CryptoResult.Failure -> return ImportSummary(
                    failedRecords = 1,
                    errors = listOf(decryptResult.message)
                )
            }
        } else {
            bundleContent
        }

        return when (val readResult = BackupManager.readBackupBundle(plainBundleString)) {
            is BackupReadResult.Success -> {
                restoreFinancialData(readResult.data)
            }
            is BackupReadResult.Failure -> {
                ImportSummary(
                    failedRecords = 1,
                    errors = listOf(readResult.message)
                )
            }
        }
    }
}
