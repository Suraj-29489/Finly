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
 * Validates safe Room database schema migration from version 2 to version 3.
 * Ensures existing expenses and budgets are preserved, and recurring expenses are fully functional.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecurringExpenseMigrationTest {

    private val dbName = "migration_v2_v3_test_db"

    @Test
    fun migrate2To3PreservesDataAndCreatesRecurringExpenses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        // 1. Create a version 2 database directly
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
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
                            `created_at` INTEGER NOT NULL
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
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_month` ON `budgets` (`month`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_category` ON `budgets` (`category`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v2Db = helper.writableDatabase

        // Insert an expense
        val expenseValues = ContentValues().apply {
            put("title", "Rent Payment")
            put("amount_in_cents", 120000L)
            put("category", "Bills")
            put("date", Instant.now().toEpochMilli())
            put("notes", "September Rent")
            put("created_at", Instant.now().toEpochMilli())
        }
        val insertedExpenseId = v2Db.insert("expenses", SQLiteDatabase.CONFLICT_REPLACE, expenseValues)
        assertTrue(insertedExpenseId > 0)

        // Insert a budget
        val budgetValues = ContentValues().apply {
            put("category", "Bills")
            put("amount_in_cents", 150000L)
            put("month", "2026-09")
            put("created_at", Instant.now().toEpochMilli())
            put("updated_at", Instant.now().toEpochMilli())
        }
        val insertedBudgetId = v2Db.insert("budgets", SQLiteDatabase.CONFLICT_REPLACE, budgetValues)
        assertTrue(insertedBudgetId > 0)
        v2Db.close()

        // 2. Open the database using Room with migrations configured
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()

        runBlocking {
            // Verify existing v2 expense data is completely intact
            val expense = roomDb.expenseDao().getExpenseById(insertedExpenseId)
            assertNotNull(expense)
            assertEquals("Rent Payment", expense?.title)
            assertEquals(120000L, expense?.amountInCents)
            assertEquals(null, expense?.recurringExpenseId)

            // Verify existing v2 budget data is completely intact
            val budget = roomDb.budgetDao().getBudgetById(insertedBudgetId)
            assertNotNull(budget)
            assertEquals(150000L, budget?.amountInCents)
            assertEquals("Bills", budget?.category)

            // Verify newly migrated recurring_expenses table supports CRUD
            val recurring = RecurringExpenseEntity(
                title = "Gym",
                amountInCents = 4500L,
                category = "Health",
                frequency = "MONTHLY",
                startDate = "2026-09-01",
                nextOccurrenceDate = "2026-10-01",
                isActive = true
            )
            val recurringId = roomDb.recurringExpenseDao().insertRecurringExpense(recurring)
            assertTrue(recurringId > 0)

            val retrievedRecurring = roomDb.recurringExpenseDao().getRecurringExpenseById(recurringId)
            assertNotNull(retrievedRecurring)
            assertEquals("Gym", retrievedRecurring?.title)
            assertEquals("MONTHLY", retrievedRecurring?.frequency)

            // Verify inserting an expense with recurring_expense_id works
            val linkedExpense = ExpenseEntity(
                title = "Gym Auto-pay",
                amountInCents = 4500L,
                category = "Health",
                date = Instant.now(),
                recurringExpenseId = recurringId
            )
            val linkedExpenseId = roomDb.expenseDao().insertExpense(linkedExpense)
            val retrievedLinked = roomDb.expenseDao().getExpenseById(linkedExpenseId)
            assertNotNull(retrievedLinked)
            assertEquals(recurringId, retrievedLinked?.recurringExpenseId)
        }

        roomDb.close()
    }
}
