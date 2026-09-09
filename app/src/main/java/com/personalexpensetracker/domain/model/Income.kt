package com.personalexpensetracker.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Domain representation of an Income transaction in Finly.
 *
 * Monetary amounts are represented using [BigDecimal] to eliminate floating-point precision issues.
 * Dates are represented using [Instant] for exact point-in-time tracking.
 */
data class Income(
    val id: Long = 0L,
    val title: String,
    val amount: BigDecimal,
    val source: String,
    val date: Instant,
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

