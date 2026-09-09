package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.BudgetEntity
import com.personalexpensetracker.domain.model.Budget
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/**
 * Mapping functions between Room budget entities and clean domain models.
 */

fun BudgetEntity.toDomain(): Budget {
    return Budget(
        id = id,
        category = category,
        amount = BigDecimal.valueOf(amountInCents, 2),
        month = YearMonth.parse(month),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Budget.toEntity(): BudgetEntity {
    val cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    return BudgetEntity(
        id = id,
        category = category,
        amountInCents = cents,
        month = month.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

