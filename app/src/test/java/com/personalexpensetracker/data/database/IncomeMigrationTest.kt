package com.personalexpensetracker.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Validates safe Room database schema migration from version 3 to version 4.
 * Ensures existing expenses, budgets, and recurring expenses are preserved,
 * and the newly created incomes table is fully functional.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IncomeMigrationTest {

    private val dbName = "migration_v3_v4_test_db"

    @Test
    fun migrate3To4PreservesDataAndCreatesIncomesTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        // 1. Create a version 3 database directly
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `expenses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `amount_in_cents` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `date` INTEGER NOT NULL,
                            `notes` TEXT,
                            `created_at` INTEGER NOT NULL,
                            `recurring_expense_id` INTEGER DEFAULT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `budgets` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `category` TEXT,
                            `amount_in_cents` INTEGER NOT NULL,
                            `month` TEXT NOT NULL,
                            `created_at` INTEGER NOT NULL,
                            `updated_at` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `amount_in_cents` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `frequency` TEXT NOT NULL,
                            `start_date` TEXT NOT NULL,
                            `next_occurrence_date` TEXT NOT NULL,
                            `is_active` INTEGER NOT NULL,
                            `end_date` TEXT,
                            `notes` TEXT,
                            `last_generated_date` TEXT,
                            `created_at` INTEGER NOT NULL,
                            `updated_at` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_recurring_expense_id` ON `expenses` (`recurring_expense_id`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_month` ON `budgets` (`month`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_category` ON `budgets` (`category`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_is_active` ON `recurring_expenses` (`is_active`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_next_occurrence_date` ON `recurring_expenses` (`next_occurrence_date`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_expenses_category` ON `recurring_expenses` (`category`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        // Insert an expense
        val expenseValues = ContentValues().apply {
            put("title", "Groceries")
            put("amount_in_cents", 4500L)
            put("category", "Food")
            put("date", Instant.now().toEpochMilli())
            put("notes", "Weekly haul")
            put("created_at", Instant.now().toEpochMilli())
        }
        val insertedExpenseId = v3Db.insert("expenses", SQLiteDatabase.CONFLICT_REPLACE, expenseValues)
        assertTrue(insertedExpenseId > 0)

        // Insert a budget
        val budgetValues = ContentValues().apply {
            put("category", "Food")
            put("amount_in_cents", 50000L)
            put("month", "2026-09")
            put("created_at", Instant.now().toEpochMilli())
            put("updated_at", Instant.now().toEpochMilli())
        }
        val insertedBudgetId = v3Db.insert("budgets", SQLiteDatabase.CONFLICT_REPLACE, budgetValues)
        assertTrue(insertedBudgetId > 0)

        // Insert a recurring expense
        val recurringValues = ContentValues().apply {
            put("title", "Netflix")
            put("amount_in_cents", 1599L)
            put("category", "Entertainment")
            put("frequency", "MONTHLY")
            put("start_date", "2026-09-01")
            put("next_occurrence_date", "2026-10-01")
            put("is_active", 1)
            put("created_at", Instant.now().toEpochMilli())
            put("updated_at", Instant.now().toEpochMilli())
        }
        val insertedRecurringId = v3Db.insert("recurring_expenses", SQLiteDatabase.CONFLICT_REPLACE, recurringValues)
        assertTrue(insertedRecurringId > 0)

        v3Db.close()

        // 2. Open the database using Room with MIGRATION_1_2, MIGRATION_2_3, and MIGRATION_3_4
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()

        runBlocking {
            // Verify existing v3 expense data is completely intact
            val expense = roomDb.expenseDao().getExpenseById(insertedExpenseId)
            assertNotNull(expense)
            assertEquals("Groceries", expense?.title)
            assertEquals(4500L, expense?.amountInCents)

            // Verify existing v3 budget data is completely intact
            val budget = roomDb.budgetDao().getBudgetById(insertedBudgetId)
            assertNotNull(budget)
            assertEquals(50000L, budget?.amountInCents)

            // Verify existing v3 recurring expense data is completely intact
            val recurring = roomDb.recurringExpenseDao().getRecurringExpenseById(insertedRecurringId)
            assertNotNull(recurring)
            assertEquals("Netflix", recurring?.title)
            assertEquals(1599L, recurring?.amountInCents)

            // Verify newly migrated incomes table supports full CRUD
            val income = IncomeEntity(
                title = "Primary Salary",
                amountInCents = 450000L,
                source = "Salary",
                date = Instant.now(),
                notes = "September pay"
            )
            val incomeId = roomDb.incomeDao().insertIncome(income)
            assertTrue(incomeId > 0)

            val retrievedIncome = roomDb.incomeDao().getIncomeById(incomeId)
            assertNotNull(retrievedIncome)
            assertEquals("Primary Salary", retrievedIncome?.title)
            assertEquals(450000L, retrievedIncome?.amountInCents)
            assertEquals("Salary", retrievedIncome?.source)
            assertEquals("September pay", retrievedIncome?.notes)
        }

        roomDb.close()
    }
}

