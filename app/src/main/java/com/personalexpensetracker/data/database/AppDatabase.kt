package com.personalexpensetracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database definition for Finly.
 * Implements a thread-safe singleton pattern using double-checked locking.
 */
@Database(
    entities = [
        ExpenseEntity::class,
        BudgetEntity::class,
        RecurringExpenseEntity::class,
        IncomeEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        private const val DATABASE_NAME = "expense_tracker_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                    "CREATE INDEX IF NOT EXISTS `index_budgets_month` ON `budgets` (`month`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_budgets_category` ON `budgets` (`category`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_expenses_is_active` ON `recurring_expenses` (`is_active`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_expenses_next_occurrence_date` ON `recurring_expenses` (`next_occurrence_date`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_expenses_category` ON `recurring_expenses` (`category`)"
                )
                db.execSQL(
                    "ALTER TABLE `expenses` ADD COLUMN `recurring_expense_id` INTEGER DEFAULT NULL"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_recurring_expense_id` ON `expenses` (`recurring_expense_id`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `incomes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount_in_cents` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `notes` TEXT,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_incomes_date` ON `incomes` (`date`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_incomes_source` ON `incomes` (`source`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
