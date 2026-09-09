package com.personalexpensetracker.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room SQLite entity representing an expense record.
 *
 * Monetary amount is stored as [Long] in minor units (cents/paise) to prevent
 * floating-point inaccuracy and support exact 64-bit integer SQL aggregations.
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["recurring_expense_id"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "amount_in_cents")
    val amountInCents: Long,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "date")
    val date: Instant,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "recurring_expense_id")
    val recurringExpenseId: Long? = null
)
