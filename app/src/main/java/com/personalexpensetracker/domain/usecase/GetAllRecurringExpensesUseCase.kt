package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe all recurring expense definitions.
 */
class GetAllRecurringExpensesUseCase(
    private val repository: RecurringExpenseRepository
) {
    operator fun invoke(): Flow<List<RecurringExpense>> {
        return repository.getAllRecurringExpenses()
    }
}

