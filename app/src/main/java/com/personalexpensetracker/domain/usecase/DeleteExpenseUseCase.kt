package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository

/**
 * Use case to delete an expense by entity or ID.
 */
class DeleteExpenseUseCase(
    private val repository: ExpenseRepository
) {

    suspend operator fun invoke(expense: Expense) {
        repository.deleteExpense(expense)
    }

    suspend operator fun invoke(id: Long) {
        repository.deleteExpenseById(id)
    }
}
