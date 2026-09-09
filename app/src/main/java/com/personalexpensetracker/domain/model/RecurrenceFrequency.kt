package com.personalexpensetracker.domain.model

/**
 * Supported recurrence frequencies for recurring expenses in Finly.
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    companion object {
        fun fromString(value: String): RecurrenceFrequency {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: MONTHLY
        }
    }
}
