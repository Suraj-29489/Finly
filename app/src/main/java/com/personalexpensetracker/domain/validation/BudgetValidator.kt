package com.personalexpensetracker.domain.validation

import java.math.BigDecimal
import java.time.YearMonth

/**
 * Domain validator ensuring business constraints on budget properties:
 * - Amount must be strictly greater than zero.
 * - Amount must not exceed reasonable maximum (100,000,000).
 * - For category budgets, category must not be blank.
 * - Month must not be null.
 */
object BudgetValidator {

    private val MAX_BUDGET = BigDecimal("100000000.00")

    fun validateMonthlyBudget(amount: BigDecimal?, month: YearMonth?): ValidationResult {
        if (month == null) {
            return ValidationResult.Invalid("Budget month is required")
        }
        if (amount == null || amount <= BigDecimal.ZERO) {
            return ValidationResult.Invalid("Budget amount must be greater than zero")
        }
        if (amount > MAX_BUDGET) {
            return ValidationResult.Invalid("Budget amount cannot exceed 100,000,000")
        }
        return ValidationResult.Valid
    }

    fun validateCategoryBudget(amount: BigDecimal?, month: YearMonth?, category: String?): ValidationResult {
        if (month == null) {
            return ValidationResult.Invalid("Budget month is required")
        }
        if (category.isNullOrBlank()) {
            return ValidationResult.Invalid("Category cannot be blank")
        }
        if (amount == null || amount <= BigDecimal.ZERO) {
            return ValidationResult.Invalid("Budget amount must be greater than zero")
        }
        if (amount > MAX_BUDGET) {
            return ValidationResult.Invalid("Budget amount cannot exceed 100,000,000")
        }
        return ValidationResult.Valid
    }
}

