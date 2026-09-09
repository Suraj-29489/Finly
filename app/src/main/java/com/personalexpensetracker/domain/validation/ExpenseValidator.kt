package com.personalexpensetracker.domain.validation

import java.math.BigDecimal

/**
 * Result of validating expense input.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

/**
 * Domain validator ensuring business constraints on expense properties:
 * - Title must not be blank.
 * - Amount must be strictly greater than zero.
 * - Category must not be blank.
 */
object ExpenseValidator {

    fun validate(title: String, amount: BigDecimal, category: String): ValidationResult {
        if (title.trim().isEmpty()) {
            return ValidationResult.Invalid("Title cannot be blank")
        }
        if (amount <= BigDecimal.ZERO) {
            return ValidationResult.Invalid("Amount must be greater than zero")
        }
        if (category.trim().isEmpty()) {
            return ValidationResult.Invalid("Category cannot be blank")
        }
        return ValidationResult.Valid
    }
}

