package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.repository.BudgetRepository
import com.personalexpensetracker.domain.validation.BudgetValidator
import com.personalexpensetracker.domain.validation.ValidationResult
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Use case to set or update a category budget for a given calendar month.
 * Enforces business rules and validation constraints before persisting.
 */
class SetCategoryBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(month: YearMonth, category: String, amount: BigDecimal): Long {
        val normalizedCategory = Category.normalize(category)
        when (val result = BudgetValidator.validateCategoryBudget(amount, month, normalizedCategory)) {
            is ValidationResult.Valid -> return repository.setCategoryBudget(month, normalizedCategory, amount)
            is ValidationResult.Invalid -> throw IllegalArgumentException(result.message)
        }
    }
}

