package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import com.personalexpensetracker.domain.validation.RecurringExpenseValidator
import com.personalexpensetracker.domain.validation.ValidationResult

/**
 * Use case to update an existing recurring expense definition.
 * Validates input and normalizes category name before persisting.
 */
class UpdateRecurringExpenseUseCase(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(recurringExpense: RecurringExpense) {
        val normalizedCategory = Category.normalize(recurringExpense.category)
        val candidate = recurringExpense.copy(category = normalizedCategory)

        when (val result = RecurringExpenseValidator.validate(
            title = candidate.title,
            amount = candidate.amount,
            category = candidate.category,
            startDate = candidate.startDate,
            endDate = candidate.endDate
        )) {
            is ValidationResult.Valid -> repository.updateRecurringExpense(candidate)
            is ValidationResult.Invalid -> throw IllegalArgumentException(result.message)
        }
    }
}

