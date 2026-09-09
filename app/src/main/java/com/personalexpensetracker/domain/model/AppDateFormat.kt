package com.personalexpensetracker.domain.model

/**
 * Supported date formats for displaying dates across Finly.
 */
enum class AppDateFormat(
    val pattern: String,
    val displayName: String,
    val sample: String
) {
    MONTH_DAY_YEAR("MMM dd, yyyy", "Month Day, Year (e.g. Jan 15, 2026)", "Jan 15, 2026"),
    DAY_MONTH_YEAR("dd/MM/yyyy", "Day/Month/Year (e.g. 15/01/2026)", "15/01/2026"),
    YEAR_MONTH_DAY("yyyy-MM-dd", "Year-Month-Day (e.g. 2026-01-15)", "2026-01-15"),
    US_SLASH("MM/dd/yyyy", "Month/Day/Year (e.g. 01/15/2026)", "01/15/2026");

    companion object {
        fun fromPattern(pattern: String?): AppDateFormat {
            return entries.firstOrNull { it.pattern == pattern } ?: MONTH_DAY_YEAR
        }
    }
}

