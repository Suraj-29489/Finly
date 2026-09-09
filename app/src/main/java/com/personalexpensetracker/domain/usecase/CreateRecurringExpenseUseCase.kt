package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import com.personalexpensetracker.domain.validation.RecurringExpenseValidator
import com.personalexpensetracker.domain.validation.ValidationResult

/**
 * Use case to create and persist a new recurring expense definition.
 * Validates input and normalizes category name before persisting.
 */
class CreateRecurringExpenseUseCase(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(recurringExpense: RecurringExpense): Long {
        val normalizedCategory = Category.normalize(recurringExpense.category)
        val candidate = recurringExpense.copy(category = normalizedCategory)

        when (val result = RecurringExpenseValidator.validate(
            title = candidate.title,
            amount = candidate.amount,
            category = candidate.category,
            startDate = candidate.startDate,
            endDate = candidate.endDate
        )) {
            is ValidationResult.Valid -> return repository.insertRecurringExpense(candidate)
            is ValidationResult.Invalid -> throw IllegalArgumentException(result.message)
        }
    }
}

