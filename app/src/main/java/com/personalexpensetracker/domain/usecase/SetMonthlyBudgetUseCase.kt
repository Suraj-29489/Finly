package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.repository.BudgetRepository
import com.personalexpensetracker.domain.validation.BudgetValidator
import com.personalexpensetracker.domain.validation.ValidationResult
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Use case to set or update the overall monthly budget for a given calendar month.
 * Enforces business rules and validation constraints before persisting.
 */
class SetMonthlyBudgetUseCase(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(month: YearMonth, amount: BigDecimal): Long {
        when (val result = BudgetValidator.validateMonthlyBudget(amount, month)) {
            is ValidationResult.Valid -> return repository.setMonthlyBudget(month, amount)
            is ValidationResult.Invalid -> throw IllegalArgumentException(result.message)
        }
    }
}

