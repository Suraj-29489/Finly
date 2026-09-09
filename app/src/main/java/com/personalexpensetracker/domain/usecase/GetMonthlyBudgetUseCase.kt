package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/**
 * Use case to observe the overall monthly budget for a given calendar month.
 */
class GetMonthlyBudgetUseCase(
    private val repository: BudgetRepository
) {
    operator fun invoke(month: YearMonth): Flow<Budget?> {
        return repository.getMonthlyBudget(month)
    }
}

