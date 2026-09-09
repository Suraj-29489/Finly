package com.personalexpensetracker.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Domain representation of an Expense.
 *
 * Monetary amounts are represented using [BigDecimal] to eliminate floating-point precision issues.
 * Dates are represented using [Instant] for exact point-in-time tracking.
 */
data class Expense(
    val id: Long = 0L,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    val date: Instant,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val recurringExpenseId: Long? = null
)
