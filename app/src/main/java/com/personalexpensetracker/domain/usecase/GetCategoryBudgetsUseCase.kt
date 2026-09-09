package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Use case to observe all category-specific budgets for a given calendar month.
 */
class GetCategoryBudgetsUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(month: YearMonth): Flow<List<Budget>> {
        return repository.getCategoryBudgets(month)
    }
}

