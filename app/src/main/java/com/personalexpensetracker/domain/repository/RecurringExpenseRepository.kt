package com.personalexpensetracker.domain.repository

import com.personalexpensetracker.domain.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface defining persistence and query contracts for RecurringExpense data.
 * Pure domain abstraction independent of underlying database technology.
 */
interface RecurringExpenseRepository {

    /**
     * Observe all recurring expense definitions ordered by next occurrence date ascending.
     */
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>

    /**
     * Observe all active recurring expense definitions.
     */
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>>

    /**
     * Observe recurring expenses for a specific [category].
     */
    fun getRecurringExpensesByCategory(category: String): Flow<List<RecurringExpense>>

    /**
     * Retrieve a recurring expense definition by its unique identifier.
     */
    suspend fun getRecurringExpenseById(id: Long): RecurringExpense?

    /**
     * Synchronously query all active recurring expenses due on or before [date].
     */
    suspend fun getDueRecurringExpenses(date: LocalDate): List<RecurringExpense>

    /**
     * Insert a new recurring expense definition and return its generated row ID.
     */
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long

    /**
     * Update an existing recurring expense definition.
     */
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense)

    /**
     * Delete an existing recurring expense definition.
     */
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)

    /**
     * Delete a recurring expense definition by its unique identifier.
     */
    suspend fun deleteRecurringExpenseById(id: Long)

    /**
     * Pause or resume a recurring expense definition by updating [isActive].
     */
    suspend fun setActive(id: Long, isActive: Boolean)

    /**
     * Update the next scheduled occurrence and last generated date when an occurrence is processed.
     */
    suspend fun updateNextOccurrence(id: Long, nextOccurrenceDate: LocalDate, lastGeneratedDate: LocalDate)
}

