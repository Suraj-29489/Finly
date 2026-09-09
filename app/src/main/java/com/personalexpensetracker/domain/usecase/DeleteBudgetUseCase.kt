package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.repository.BudgetRepository
import java.time.YearMonth

/**
 * Use case to remove/delete budgets:
 * - by budget ID
 * - by domain model instance
 * - by month (overall monthly budget)
 * - by month and category (category budget)
 */
class DeleteBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend fun byId(id: Long) {
        repository.deleteBudgetById(id)
    }

    suspend fun budget(budget: Budget) {
        repository.deleteBudget(budget)
    }

    suspend fun monthlyBudget(month: YearMonth) {
        repository.deleteMonthlyBudget(month)
    }

    suspend fun categoryBudget(month: YearMonth, category: String) {
        repository.deleteCategoryBudget(month, category)
    }
}

