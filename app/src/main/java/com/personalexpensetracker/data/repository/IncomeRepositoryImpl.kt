package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.IncomeDao
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Concrete implementation of [IncomeRepository] utilizing [IncomeDao] for local persistence.
 */
class IncomeRepositoryImpl(
    private val incomeDao: IncomeDao
) : IncomeRepository {

    override fun getAllIncomes(): Flow<List<Income>> {
        return incomeDao.getAllIncomes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getIncomeById(id: Long): Income? {
        return incomeDao.getIncomeById(id)?.toDomain()
    }

    override suspend fun insertIncome(income: Income): Long {
        return incomeDao.insertIncome(income.toEntity())
    }

    override suspend fun updateIncome(income: Income) {
        incomeDao.updateIncome(income.toEntity())
    }

    override suspend fun deleteIncome(income: Income) {
        incomeDao.deleteIncome(income.toEntity())
    }

    override suspend fun deleteIncomeById(id: Long) {
        incomeDao.deleteIncomeById(id)
    }

    override fun getIncomesBySource(source: String): Flow<List<Income>> {
        return incomeDao.getIncomesBySource(source).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> {
        return incomeDao.getIncomesByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalIncomeInCents(): Flow<Long?> {
        return incomeDao.getTotalIncomeInCents()
    }
}

