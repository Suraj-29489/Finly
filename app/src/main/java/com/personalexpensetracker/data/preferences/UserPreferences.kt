package com.personalexpensetracker.data.preferences

import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import com.personalexpensetracker.domain.model.UserPreferencesData
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract for persisting and retrieving user preferences.
 */
interface UserPreferences {
    val preferencesFlow: Flow<UserPreferencesData>
    fun getPreferences(): UserPreferencesData

    suspend fun setCurrency(currency: AppCurrency)
    suspend fun setThemeMode(themeMode: AppThemeMode)
    suspend fun setDateFormat(dateFormat: AppDateFormat)
    suspend fun setDefaultCategory(category: String)
    suspend fun setDefaultSortOrder(sortOrder: String)
    suspend fun setAutoGenerateRecurring(enabled: Boolean)
    suspend fun setAutoCaptureExpenses(enabled: Boolean)
    suspend fun setAutoCaptureSms(enabled: Boolean)
    suspend fun resetToDefaults()
}
