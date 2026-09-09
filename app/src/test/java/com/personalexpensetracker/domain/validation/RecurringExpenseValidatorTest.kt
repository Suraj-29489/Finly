package com.personalexpensetracker.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurringExpenseValidatorTest {

    private val startDate = LocalDate.of(2026, 9, 1)

    @Test
    fun validRecurringExpenseReturnsValid() {
        val result = RecurringExpenseValidator.validate(
            title = "Gym Membership",
            amount = BigDecimal("50.00"),
            category = "Health",
            startDate = startDate,
            endDate = null
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun blankTitleReturnsInvalid() {
        val result = RecurringExpenseValidator.validate(
            title = "   ",
            amount = BigDecimal("50.00"),
            category = "Health",
            startDate = startDate,
            endDate = null
        )
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Title cannot be blank", (result as ValidationResult.Invalid).message)
    }

    @Test
    fun zeroOrNegativeAmountReturnsInvalid() {
        val zeroResult = RecurringExpenseValidator.validate(
            title = "Subscription",
            amount = BigDecimal.ZERO,
            category = "Bills",
            startDate = startDate,
            endDate = null
        )
        assertTrue(zeroResult is ValidationResult.Invalid)

        val negativeResult = RecurringExpenseValidator.validate(
            title = "Subscription",
            amount = BigDecimal("-10.00"),
            category = "Bills",
            startDate = startDate,
            endDate = null
        )
        assertTrue(negativeResult is ValidationResult.Invalid)
    }

    @Test
    fun excessiveAmountReturnsInvalid() {
        val result = RecurringExpenseValidator.validate(
            title = "Mansion",
            amount = BigDecimal("100000001.00"),
            category = "Bills",
            startDate = startDate,
            endDate = null
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun blankCategoryReturnsInvalid() {
        val result = RecurringExpenseValidator.validate(
            title = "Subscription",
            amount = BigDecimal("10.00"),
            category = "",
            startDate = startDate,
            endDate = null
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun nullStartDateReturnsInvalid() {
        val result = RecurringExpenseValidator.validate(
            title = "Subscription",
            amount = BigDecimal("10.00"),
            category = "Bills",
            startDate = null,
            endDate = null
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun endDateBeforeStartDateReturnsInvalid() {
        val result = RecurringExpenseValidator.validate(
            title = "Subscription",
            amount = BigDecimal("10.00"),
            category = "Bills",
            startDate = startDate,
            endDate = startDate.minusDays(1)
        )
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("End date cannot be before start date", (result as ValidationResult.Invalid).message)
    }

    @Test
    fun validEndDateReturnsValid() {
        val result = RecurringExpenseValidator.validate(
            title = "Subscription",
            amount = BigDecimal("10.00"),
            category = "Bills",
            startDate = startDate,
            endDate = startDate.plusMonths(6)
        )
        assertTrue(result is ValidationResult.Valid)
    }
}

