package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.validation.ExpenseValidator
import com.personalexpensetracker.domain.validation.ValidationResult

/**
 * Use case to validate and persist a new expense.
 *
 * Throws [IllegalArgumentException] if validation fails.
 */
class AddExpenseUseCase(
    private val repository: ExpenseRepository
) {

    suspend operator fun invoke(expense: Expense): Long {
        when (val result = ExpenseValidator.validate(expense.title, expense.amount, expense.category)) {
            is ValidationResult.Invalid -> throw IllegalArgumentException(result.message)
            ValidationResult.Valid -> return repository.insertExpense(expense)
        }
    }
}
