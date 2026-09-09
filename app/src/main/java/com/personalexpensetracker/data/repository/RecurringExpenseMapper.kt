package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.RecurringExpenseEntity
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Mapping functions between Room recurring expense entities and domain models.
 */

fun RecurringExpenseEntity.toDomain(): RecurringExpense {
    return RecurringExpense(
        id = id,
        title = title,
        amount = BigDecimal.valueOf(amountInCents, 2),
        category = category,
        frequency = RecurrenceFrequency.fromString(frequency),
        startDate = LocalDate.parse(startDate),
        nextOccurrenceDate = LocalDate.parse(nextOccurrenceDate),
        isActive = isActive,
        endDate = endDate?.let { LocalDate.parse(it) },
        notes = notes,
        lastGeneratedDate = lastGeneratedDate?.let { LocalDate.parse(it) },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RecurringExpense.toEntity(): RecurringExpenseEntity {
    val cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    return RecurringExpenseEntity(
        id = id,
        title = title,
        amountInCents = cents,
        category = category,
        frequency = frequency.name,
        startDate = startDate.toString(),
        nextOccurrenceDate = nextOccurrenceDate.toString(),
        isActive = isActive,
        endDate = endDate?.toString(),
        notes = notes,
        lastGeneratedDate = lastGeneratedDate?.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

