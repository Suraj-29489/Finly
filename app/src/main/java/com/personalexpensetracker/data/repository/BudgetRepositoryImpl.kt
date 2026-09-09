package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.BudgetDao
import com.personalexpensetracker.data.database.BudgetEntity
import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth

/**
 * Concrete implementation of [BudgetRepository] utilizing [BudgetDao] for Room persistence.
 */
class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBudgetsForMonth(month: YearMonth): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(month.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMonthlyBudget(month: YearMonth): Flow<Budget?> {
        return budgetDao.getMonthlyBudget(month.toString()).map { it?.toDomain() }
    }

    override fun getCategoryBudgets(month: YearMonth): Flow<List<Budget>> {
        return budgetDao.getCategoryBudgets(month.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategoryBudget(month: YearMonth, category: String): Flow<Budget?> {
        return budgetDao.getCategoryBudget(month.toString(), category).map { it?.toDomain() }
    }

    override suspend fun getBudgetById(id: Long): Budget? {
        return budgetDao.getBudgetById(id)?.toDomain()
    }

    override suspend fun setMonthlyBudget(month: YearMonth, amount: BigDecimal): Long {
        val monthStr = month.toString()
        val cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        val now = Instant.now()
        val existing = budgetDao.getMonthlyBudgetSync(monthStr)
        return if (existing != null) {
            val updated = existing.copy(
                amountInCents = cents,
                updatedAt = now
            )
            budgetDao.updateBudget(updated)
            existing.id
        } else {
            val newEntity = BudgetEntity(
                category = null,
                amountInCents = cents,
                month = monthStr,
                createdAt = now,
                updatedAt = now
            )
            budgetDao.insertBudget(newEntity)
        }
    }

    override suspend fun setCategoryBudget(month: YearMonth, category: String, amount: BigDecimal): Long {
        val monthStr = month.toString()
        val cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
        val now = Instant.now()
        val existing = budgetDao.getCategoryBudgetSync(monthStr, category)
        return if (existing != null) {
            val updated = existing.copy(
                amountInCents = cents,
                updatedAt = now
            )
            budgetDao.updateBudget(updated)
            existing.id
        } else {
            val newEntity = BudgetEntity(
                category = category,
                amountInCents = cents,
                month = monthStr,
                createdAt = now,
                updatedAt = now
            )
            budgetDao.insertBudget(newEntity)
        }
    }

    override suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget.toEntity().copy(updatedAt = Instant.now()))
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget.toEntity())
    }

    override suspend fun deleteBudgetById(id: Long) {
        budgetDao.deleteBudgetById(id)
    }

    override suspend fun deleteMonthlyBudget(month: YearMonth) {
        budgetDao.deleteMonthlyBudget(month.toString())
    }

    override suspend fun deleteCategoryBudget(month: YearMonth, category: String) {
        budgetDao.deleteCategoryBudget(month.toString(), category)
    }
}

