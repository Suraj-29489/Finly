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
 * Data Access Object for recurring_expenses table operations in Finly.
 */
@Dao
interface RecurringExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpenses(recurringExpenses: List<RecurringExpenseEntity>): List<Long>

    @Update
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpenseEntity): Int

    @Delete
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpenseEntity): Int

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: Long): Int

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses(): Int

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses ORDER BY next_occurrence_date ASC, id ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses ORDER BY next_occurrence_date ASC, id ASC")
    suspend fun getAllRecurringExpensesSync(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE is_active = 1 ORDER BY next_occurrence_date ASC")
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE is_active = 1 ORDER BY next_occurrence_date ASC")
    suspend fun getActiveRecurringExpensesSync(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE is_active = 1 AND next_occurrence_date <= :date ORDER BY next_occurrence_date ASC")
    suspend fun getDueRecurringExpenses(date: String): List<RecurringExpenseEntity>

    @Query("UPDATE recurring_expenses SET is_active = :isActive, updated_at = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean, updatedAt: Instant): Int

    @Query("UPDATE recurring_expenses SET next_occurrence_date = :nextDate, last_generated_date = :lastDate, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateOccurrence(id: Long, nextDate: String, lastDate: String, updatedAt: Instant): Int

    @Query("SELECT * FROM recurring_expenses WHERE category = :category ORDER BY next_occurrence_date ASC")
    fun getRecurringExpensesByCategory(category: String): Flow<List<RecurringExpenseEntity>>
}

