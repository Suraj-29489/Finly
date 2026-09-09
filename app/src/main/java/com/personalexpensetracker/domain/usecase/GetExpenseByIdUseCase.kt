package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository

/**
 * Use case to retrieve a single expense by its unique identifier.
 */
class GetExpenseByIdUseCase(
    private val repository: ExpenseRepository
) {

    suspend operator fun invoke(id: Long): Expense? {
        return repository.getExpenseById(id)
    }
}

