package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository

/**
 * Use case to retrieve a recurring expense definition by its ID.
 */
class GetRecurringExpenseByIdUseCase(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(id: Long): RecurringExpense? {
        return repository.getRecurringExpenseById(id)
    }
}

