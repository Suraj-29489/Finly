package com.personalexpensetracker.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class IncomeValidatorTest {

    private val now = Instant.now()

    @Test
    fun validIncomeReturnsValid() {
        val result = IncomeValidator.validate(
            title = "Monthly Salary",
            amount = BigDecimal("5000.00"),
            source = "Salary",
            date = now
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun validIncomeConvenienceOverloadReturnsValid() {
        val result = IncomeValidator.validate(
            title = "Bonus",
            amount = BigDecimal("1000.00"),
            source = "Salary"
        )
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun blankTitleReturnsInvalid() {
        val emptyResult = IncomeValidator.validate(
            title = "",
            amount = BigDecimal("5000.00"),
            source = "Salary",
            date = now
        )
        assertTrue(emptyResult is ValidationResult.Invalid)
        assertEquals("Title cannot be blank", (emptyResult as ValidationResult.Invalid).message)

        val whitespaceResult = IncomeValidator.validate(
            title = "   ",
            amount = BigDecimal("5000.00"),
            source = "Salary",
            date = now
        )
        assertTrue(whitespaceResult is ValidationResult.Invalid)
        assertEquals("Title cannot be blank", (whitespaceResult as ValidationResult.Invalid).message)
    }

    @Test
    fun zeroOrNegativeAmountReturnsInvalid() {
        val zeroResult = IncomeValidator.validate(
            title = "Salary",
            amount = BigDecimal.ZERO,
            source = "Salary",
            date = now
        )
        assertTrue(zeroResult is ValidationResult.Invalid)
        assertEquals("Amount must be greater than zero", (zeroResult as ValidationResult.Invalid).message)

        val negativeResult = IncomeValidator.validate(
            title = "Salary",
            amount = BigDecimal("-100.00"),
            source = "Salary",
            date = now
        )
        assertTrue(negativeResult is ValidationResult.Invalid)
        assertEquals("Amount must be greater than zero", (negativeResult as ValidationResult.Invalid).message)

        val nullResult = IncomeValidator.validate(
            title = "Salary",
            amount = null,
            source = "Salary",
            date = now
        )
        assertTrue(nullResult is ValidationResult.Invalid)
        assertEquals("Amount must be greater than zero", (nullResult as ValidationResult.Invalid).message)
    }

    @Test
    fun excessiveAmountReturnsInvalid() {
        val excessiveResult = IncomeValidator.validate(
            title = "Lottery",
            amount = BigDecimal("100000000.01"),
            source = "Other",
            date = now
        )
        assertTrue(excessiveResult is ValidationResult.Invalid)
        assertEquals("Amount cannot exceed 100,000,000", (excessiveResult as ValidationResult.Invalid).message)

        val exactMaxResult = IncomeValidator.validate(
            title = "Mega Sale",
            amount = BigDecimal("100000000.00"),
            source = "Business",
            date = now
        )
        assertTrue(exactMaxResult is ValidationResult.Valid)
    }

    @Test
    fun blankSourceReturnsInvalid() {
        val emptySourceResult = IncomeValidator.validate(
            title = "Consulting",
            amount = BigDecimal("2000.00"),
            source = "",
            date = now
        )
        assertTrue(emptySourceResult is ValidationResult.Invalid)
        assertEquals("Income source cannot be blank", (emptySourceResult as ValidationResult.Invalid).message)

        val whitespaceSourceResult = IncomeValidator.validate(
            title = "Consulting",
            amount = BigDecimal("2000.00"),
            source = "   ",
            date = now
        )
        assertTrue(whitespaceSourceResult is ValidationResult.Invalid)
        assertEquals("Income source cannot be blank", (whitespaceSourceResult as ValidationResult.Invalid).message)
    }

    @Test
    fun nullDateReturnsInvalid() {
        val result = IncomeValidator.validate(
            title = "Gift",
            amount = BigDecimal("50.00"),
            source = "Gift",
            date = null
        )
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Date is required", (result as ValidationResult.Invalid).message)
    }
}

