package com.personalexpensetracker.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Domain representation of a Recurring Expense definition in Finly.
 *
 * Tracks recurring subscriptions, bills, rent, memberships, and regular expenses.
 * Dates are tracked as [LocalDate] to align with calendar cycles and avoid timezone shifts.
 */
data class RecurringExpense(
    val id: Long = 0L,
    val title: String,
    val amount: BigDecimal,
    val category: String,
    val frequency: RecurrenceFrequency,
    val startDate: LocalDate,
    val nextOccurrenceDate: LocalDate,
    val isActive: Boolean = true,
    val endDate: LocalDate? = null,
    val notes: String? = null,
    val lastGeneratedDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    /**
     * Checks if this recurring expense has passed its optional end date.
     */
    fun isExpired(currentDate: LocalDate): Boolean {
        return endDate != null && currentDate.isAfter(endDate)
    }

    /**
     * Checks whether an occurrence is currently due on or before [currentDate].
     */
    fun isDue(currentDate: LocalDate): Boolean {
        if (!isActive) return false
        if (isExpired(currentDate)) return false
        return !nextOccurrenceDate.isAfter(currentDate)
    }
}
