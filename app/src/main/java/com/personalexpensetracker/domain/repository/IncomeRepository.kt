package com.personalexpensetracker.domain.repository

import com.personalexpensetracker.domain.model.Income
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository interface defining persistence and query contracts for Income data.
 * Pure domain abstraction independent of underlying database technology.
 */
interface IncomeRepository {

    /**
     * Observe all recorded income entries ordered by date descending.
     */
    fun getAllIncomes(): Flow<List<Income>>

    /**
     * Retrieve a specific income entry by unique identifier.
     */
    suspend fun getIncomeById(id: Long): Income?

    /**
     * Insert a new income entry and return its generated row ID.
     */
    suspend fun insertIncome(income: Income): Long

    /**
     * Update an existing income entry.
     */
    suspend fun updateIncome(income: Income)

    /**
     * Delete an existing income entry.
     */
    suspend fun deleteIncome(income: Income)

    /**
     * Delete an income entry by unique identifier.
     */
    suspend fun deleteIncomeById(id: Long)

    /**
     * Observe income entries belonging to a specific source/category.
     */
    fun getIncomesBySource(source: String): Flow<List<Income>>

    /**
     * Observe income entries occurring within an inclusive date range.
     */
    fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>>

    /**
     * Observe total income in minor currency units (cents).
     */
    fun getTotalIncomeInCents(): Flow<Long?>
}

