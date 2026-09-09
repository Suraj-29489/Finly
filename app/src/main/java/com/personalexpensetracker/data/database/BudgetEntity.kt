package com.personalexpensetracker.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room SQLite entity representing a budget record in Finly.
 *
 * Supports overall monthly budgets (where [category] is null) and category budgets.
 * Monetary amount is stored as [Long] in minor units (cents) to maintain exact 64-bit integer calculations.
 * Period is stored as an ISO-8601 year-month string ([month], e.g. "2026-09").
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["month"]),
        Index(value = ["category"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "amount_in_cents")
    val amountInCents: Long,

    @ColumnInfo(name = "month")
    val month: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)

