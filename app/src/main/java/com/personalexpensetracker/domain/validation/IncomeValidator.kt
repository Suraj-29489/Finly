package com.personalexpensetracker.domain.validation

import java.math.BigDecimal
import java.time.Instant

/**
 * Domain validator ensuring business constraints on income transactions:
 * - Title must not be blank.
 * - Amount must be strictly greater than zero and not exceed 100,000,000.
 * - Source must not be blank.
 * - Date must not be null.
 */
object IncomeValidator {

    private val MAX_AMOUNT = BigDecimal("100000000.00")

    fun validate(
        title: String,
        amount: BigDecimal?,
        source: String,
        date: Instant? = null
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
        if (source.trim().isEmpty()) {
            return ValidationResult.Invalid("Income source cannot be blank")
        }
        if (date == null) {
            return ValidationResult.Invalid("Date is required")
        }
        return ValidationResult.Valid
    }

    /**
     * Convenience overload omitting date if defaulting to current time.
     */
    fun validate(
        title: String,
        amount: BigDecimal?,
        source: String
    ): ValidationResult {
        return validate(title, amount, source, Instant.now())
    }
}

