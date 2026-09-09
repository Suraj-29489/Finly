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
 * Validates safe Room database schema migration from version 1 to 2.
 * Ensures existing expense data is preserved and new budgets table is fully operational.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetMigrationTest {

    private val dbName = "migration_test_db"

    @Test
    fun migrate1To2PreservesExpensesAndCreatesBudgetsTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        // 1. Create a version 1 database directly and insert an expense
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
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
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        val nowEpoch = Instant.now().toEpochMilli()
        val values = ContentValues().apply {
            put("title", "Supermarket Groceries")
            put("amount_in_cents", 4500L)
            put("category", "Food")
            put("date", nowEpoch)
            put("notes", "Weekly supplies")
            put("created_at", nowEpoch)
        }
        val insertedExpenseId = v1Db.insert("expenses", SQLiteDatabase.CONFLICT_REPLACE, values)
        assertTrue(insertedExpenseId > 0)
        v1Db.close()

        // 2. Open the database using Room with migrations configured
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()

        runBlocking {
            // Verify existing v1 expense data is completely intact
            val expense = roomDb.expenseDao().getExpenseById(insertedExpenseId)
            assertNotNull(expense)
            assertEquals("Supermarket Groceries", expense?.title)
            assertEquals(4500L, expense?.amountInCents)
            assertEquals("Food", expense?.category)
            assertEquals(nowEpoch, expense?.date?.toEpochMilli())

            // Verify newly migrated budgets table supports full CRUD
            val budget = BudgetEntity(
                category = null,
                amountInCents = 150000L,
                month = "2026-09"
            )
            val budgetId = roomDb.budgetDao().insertBudget(budget)
            assertTrue(budgetId > 0)

            val retrievedBudget = roomDb.budgetDao().getBudgetById(budgetId)
            assertNotNull(retrievedBudget)
            assertEquals(150000L, retrievedBudget?.amountInCents)
            assertEquals("2026-09", retrievedBudget?.month)
        }

        roomDb.close()
    }
}

