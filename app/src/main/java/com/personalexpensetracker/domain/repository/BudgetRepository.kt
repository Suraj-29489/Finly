package com.personalexpensetracker.domain.repository

import com.personalexpensetracker.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Repository interface defining persistence and query contracts for Budget data.
 * Pure domain abstraction independent of underlying database technology.
 */
interface BudgetRepository {

    /**
     * Observe all budgets recorded across all months.
     */
    fun getAllBudgets(): Flow<List<Budget>>

    /**
     * Observe all budgets (overall and category) for a specific [month].
     */
    fun getBudgetsForMonth(month: YearMonth): Flow<List<Budget>>

    /**
     * Observe the overall monthly budget for a specific [month].
     */
    fun getMonthlyBudget(month: YearMonth): Flow<Budget?>

    /**
     * Observe all category-specific budgets for a specific [month].
     */
    fun getCategoryBudgets(month: YearMonth): Flow<List<Budget>>

    /**
     * Observe a specific category budget for a [month] and [category].
     */
    fun getCategoryBudget(month: YearMonth, category: String): Flow<Budget?>

    /**
     * Retrieve a budget by its unique identifier.
     */
    suspend fun getBudgetById(id: Long): Budget?

    /**
     * Set or update the overall monthly budget for a specific [month].
     * Returns the budget row ID.
     */
    suspend fun setMonthlyBudget(month: YearMonth, amount: BigDecimal): Long

    /**
     * Set or update a category budget for a specific [month] and [category].
     * Returns the budget row ID.
     */
    suspend fun setCategoryBudget(month: YearMonth, category: String, amount: BigDecimal): Long

    /**
     * Insert a new budget record and return its row ID.
     */
    suspend fun insertBudget(budget: Budget): Long

    /**
     * Update an existing budget record.
     */
    suspend fun updateBudget(budget: Budget)

    /**
     * Delete an existing budget record.
     */
    suspend fun deleteBudget(budget: Budget)

    /**
     * Delete a budget by its unique identifier.
     */
    suspend fun deleteBudgetById(id: Long)

    /**
     * Delete the overall monthly budget for a specific [month].
     */
    suspend fun deleteMonthlyBudget(month: YearMonth)

    /**
     * Delete a category budget for a specific [month] and [category].
     */
    suspend fun deleteCategoryBudget(month: YearMonth, category: String)
}

