package com.personalexpensetracker.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room SQLite entity representing an income transaction.
 *
 * Monetary amount is stored as [Long] in minor units (cents/paise) to prevent
 * floating-point inaccuracy and support exact 64-bit integer SQL aggregations.
 */
@Entity(
    tableName = "incomes",
    indices = [
        Index(value = ["date"]),
        Index(value = ["source"])
    ]
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "amount_in_cents")
    val amountInCents: Long,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "date")
    val date: Instant,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now()
)

