package com.personalexpensetracker.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room SQLite entity representing a recurring expense definition in Finly.
 *
 * Monetary amount is stored as [Long] in minor units (cents).
 * Dates are stored in ISO-8601 calendar date format ("YYYY-MM-DD") for deterministic SQL queries.
 */
@Entity(
    tableName = "recurring_expenses",
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["next_occurrence_date"]),
        Index(value = ["category"])
    ]
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "amount_in_cents")
    val amountInCents: Long,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "frequency")
    val frequency: String,

    @ColumnInfo(name = "start_date")
    val startDate: String,

    @ColumnInfo(name = "next_occurrence_date")
    val nextOccurrenceDate: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "end_date")
    val endDate: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "last_generated_date")
    val lastGeneratedDate: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)

