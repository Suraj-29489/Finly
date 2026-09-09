package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.ExpenseEntity
import com.personalexpensetracker.domain.model.Expense
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Mapping functions between Room database entities and clean domain models.
 */

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        title = title,
        amount = BigDecimal.valueOf(amountInCents, 2),
        category = category,
        date = date,
        notes = notes,
        createdAt = createdAt,
        recurringExpenseId = recurringExpenseId
    )
}

fun Expense.toEntity(): ExpenseEntity {
    val cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    return ExpenseEntity(
        id = id,
        title = title,
        amountInCents = cents,
        category = category,
        date = date,
        notes = notes,
        createdAt = createdAt,
        recurringExpenseId = recurringExpenseId
    )
}
