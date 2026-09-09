package com.personalexpensetracker.data.repository

import com.personalexpensetracker.data.database.IncomeEntity
import com.personalexpensetracker.domain.model.Income
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Mapping functions between Room database income entities and clean domain models.
 */

fun IncomeEntity.toDomain(): Income {
    return Income(
        id = id,
        title = title,
        amount = BigDecimal.valueOf(amountInCents, 2),
        source = source,
        date = date,
        notes = notes,
        createdAt = createdAt
    )
}

fun Income.toEntity(): IncomeEntity {
    val cents = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    return IncomeEntity(
        id = id,
        title = title,
        amountInCents = cents,
        source = source,
        date = date,
        notes = notes,
        createdAt = createdAt
    )
}

