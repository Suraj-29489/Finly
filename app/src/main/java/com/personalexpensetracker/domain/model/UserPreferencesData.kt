package com.personalexpensetracker.domain.model

/**
 * Immutable representation of persistent user preferences in Finly.
 */
data class UserPreferencesData(
    val currency: AppCurrency = AppCurrency.USD,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dateFormat: AppDateFormat = AppDateFormat.MONTH_DAY_YEAR,
    val defaultCategory: String = "Food",
    val defaultSortOrder: String = "NEWEST_FIRST",
    val autoGenerateRecurring: Boolean = true,
    val autoCaptureExpenses: Boolean = false,
    val autoCaptureSms: Boolean = false
)
