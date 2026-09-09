package com.personalexpensetracker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for expenses table operations.
 */
@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity): Int

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long): Int

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses(): Int

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    fun getExpensesByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount_in_cents) FROM expenses")
    fun getTotalExpensesInCents(): Flow<Long?>

    @Query("SELECT * FROM expenses WHERE recurring_expense_id = :recurringId ORDER BY date DESC")
    fun getExpensesByRecurringId(recurringId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE recurring_expense_id = :recurringId ORDER BY date DESC")
    suspend fun getExpensesByRecurringIdSync(recurringId: Long): List<ExpenseEntity>
}
