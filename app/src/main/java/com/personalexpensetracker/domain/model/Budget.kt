package com.personalexpensetracker.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

/**
 * Domain representation of a spending Budget in Finly.
 *
 * Supports both:
 * - Overall monthly budgets (where [category] is null)
 * - Category-specific budgets (where [category] is the category name, e.g. "Food")
 *
 * Monetary amounts are represented using [BigDecimal] with 2 decimal places to ensure precision.
 * Budget period is tracked using [YearMonth] for clean calendar month association.
 */
data class Budget(
    val id: Long = 0L,
    val category: String? = null,
    val amount: BigDecimal,
    val month: YearMonth,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * True if this budget applies to all expenses in the month.
     */
    val isOverall: Boolean
        get() = category == null

    /**
     * True if this budget applies to a specific category.
     */
    val isCategoryBudget: Boolean
        get() = category != null
}

