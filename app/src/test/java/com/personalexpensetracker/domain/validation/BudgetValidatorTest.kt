package com.personalexpensetracker.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

class BudgetValidatorTest {

    private val validMonth = YearMonth.of(2026, 9)

    @Test
    fun validMonthlyBudgetReturnsValid() {
        val result = BudgetValidator.validateMonthlyBudget(BigDecimal("1500.00"), validMonth)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun nullMonthReturnsInvalid() {
        val result = BudgetValidator.validateMonthlyBudget(BigDecimal("100.00"), null)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Budget month is required", (result as ValidationResult.Invalid).message)
    }

    @Test
    fun zeroOrNegativeMonthlyBudgetReturnsInvalid() {
        val zeroResult = BudgetValidator.validateMonthlyBudget(BigDecimal.ZERO, validMonth)
        assertTrue(zeroResult is ValidationResult.Invalid)

        val negativeResult = BudgetValidator.validateMonthlyBudget(BigDecimal("-50.00"), validMonth)
        assertTrue(negativeResult is ValidationResult.Invalid)
    }

    @Test
    fun excessiveBudgetReturnsInvalid() {
        val excessive = BigDecimal("100000001.00")
        val result = BudgetValidator.validateMonthlyBudget(excessive, validMonth)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun validCategoryBudgetReturnsValid() {
        val result = BudgetValidator.validateCategoryBudget(BigDecimal("400.00"), validMonth, "Food")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun blankCategoryBudgetReturnsInvalid() {
        val emptyResult = BudgetValidator.validateCategoryBudget(BigDecimal("400.00"), validMonth, "")
        assertTrue(emptyResult is ValidationResult.Invalid)

        val spacesResult = BudgetValidator.validateCategoryBudget(BigDecimal("400.00"), validMonth, "   ")
        assertTrue(spacesResult is ValidationResult.Invalid)

        val nullResult = BudgetValidator.validateCategoryBudget(BigDecimal("400.00"), validMonth, null)
        assertTrue(nullResult is ValidationResult.Invalid)
    }
}

