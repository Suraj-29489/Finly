package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.RecurringExpenseDao
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/**
 * Concrete implementation of [RecurringExpenseRepository] utilizing [RecurringExpenseDao] for local persistence.
 */
class RecurringExpenseRepositoryImpl(
    private val recurringExpenseDao: RecurringExpenseDao
) : RecurringExpenseRepository {

    override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> {
        return recurringExpenseDao.getAllRecurringExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> {
        return recurringExpenseDao.getActiveRecurringExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecurringExpensesByCategory(category: String): Flow<List<RecurringExpense>> {
        return recurringExpenseDao.getRecurringExpensesByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRecurringExpenseById(id: Long): RecurringExpense? {
        return recurringExpenseDao.getRecurringExpenseById(id)?.toDomain()
    }

    override suspend fun getDueRecurringExpenses(date: LocalDate): List<RecurringExpense> {
        return recurringExpenseDao.getDueRecurringExpenses(date.toString()).map { it.toDomain() }
    }

    override suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long {
        return recurringExpenseDao.insertRecurringExpense(recurringExpense.toEntity())
    }

    override suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) {
        val entity = recurringExpense.toEntity().copy(updatedAt = Instant.now())
        recurringExpenseDao.updateRecurringExpense(entity)
    }

    override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.deleteRecurringExpense(recurringExpense.toEntity())
    }

    override suspend fun deleteRecurringExpenseById(id: Long) {
        recurringExpenseDao.deleteRecurringExpenseById(id)
    }

    override suspend fun setActive(id: Long, isActive: Boolean) {
        recurringExpenseDao.setActive(id, isActive, Instant.now())
    }

    override suspend fun updateNextOccurrence(id: Long, nextOccurrenceDate: LocalDate, lastGeneratedDate: LocalDate) {
        recurringExpenseDao.updateOccurrence(id, nextOccurrenceDate.toString(), lastGeneratedDate.toString(), Instant.now())
    }
}

