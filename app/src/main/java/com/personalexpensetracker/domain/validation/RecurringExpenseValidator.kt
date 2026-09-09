package com.personalexpensetracker.domain.validation

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Domain validator ensuring business constraints on recurring expense definitions:
 * - Title must not be blank.
 * - Amount must be strictly greater than zero and not exceed 100,000,000.
 * - Category must not be blank.
 * - Start date must not be null.
 * - End date, if provided, must not be before start date.
 */
object RecurringExpenseValidator {

    private val MAX_AMOUNT = BigDecimal("100000000.00")

    fun validate(
        title: String,
        amount: BigDecimal?,
        category: String,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): ValidationResult {
        if (title.trim().isEmpty()) {
            return ValidationResult.Invalid("Title cannot be blank")
        }
        if (amount == null || amount <= BigDecimal.ZERO) {
            return ValidationResult.Invalid("Amount must be greater than zero")
        }
        if (amount > MAX_AMOUNT) {
            return ValidationResult.Invalid("Amount cannot exceed 100,000,000")
        }
        if (category.trim().isEmpty()) {
            return ValidationResult.Invalid("Category cannot be blank")
        }
        if (startDate == null) {
            return ValidationResult.Invalid("Start date is required")
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            return ValidationResult.Invalid("End date cannot be before start date")
        }
        return ValidationResult.Valid
    }
}

