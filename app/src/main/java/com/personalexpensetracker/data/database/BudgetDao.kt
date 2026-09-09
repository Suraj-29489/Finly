package com.personalexpensetracker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for budgets table operations in Finly.
 */
@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>): List<Long>

    @Update
    suspend fun updateBudget(budget: BudgetEntity): Int

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity): Int

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long): Int

    @Query("DELETE FROM budgets WHERE month = :month AND category IS NULL")
    suspend fun deleteMonthlyBudget(month: String): Int

    @Query("DELETE FROM budgets WHERE month = :month AND category = :category")
    suspend fun deleteCategoryBudget(month: String, category: String): Int

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets(): Int

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE month = :month AND category IS NULL LIMIT 1")
    fun getMonthlyBudget(month: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month AND category IS NULL LIMIT 1")
    suspend fun getMonthlyBudgetSync(month: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE month = :month AND category = :category LIMIT 1")
    fun getCategoryBudget(month: String, category: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month = :month AND category = :category LIMIT 1")
    suspend fun getCategoryBudgetSync(month: String, category: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE month = :month AND category IS NOT NULL ORDER BY category ASC")
    fun getCategoryBudgets(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month ORDER BY category IS NOT NULL, category ASC")
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY month DESC, category IS NOT NULL, category ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY month DESC, category IS NOT NULL, category ASC")
    suspend fun getAllBudgetsSync(): List<BudgetEntity>
}

