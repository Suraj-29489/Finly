package com.personalexpensetracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.data.preferences.UserPreferences
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import com.personalexpensetracker.domain.model.UserPreferencesData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val preferencesState: StateFlow<UserPreferencesData> = userPreferences.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = userPreferences.getPreferences()
        )

    fun selectCurrency(currency: AppCurrency) {
        viewModelScope.launch {
            userPreferences.setCurrency(currency)
        }
    }

    fun selectThemeMode(themeMode: AppThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(themeMode)
        }
    }

    fun selectDateFormat(dateFormat: AppDateFormat) {
        viewModelScope.launch {
            userPreferences.setDateFormat(dateFormat)
        }
    }

    fun selectDefaultCategory(category: String) {
        viewModelScope.launch {
            userPreferences.setDefaultCategory(category)
        }
    }

    fun selectDefaultSortOrder(sortOrder: String) {
        viewModelScope.launch {
            userPreferences.setDefaultSortOrder(sortOrder)
        }
    }

    fun toggleAutoGenerateRecurring(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoGenerateRecurring(enabled)
        }
    }

    fun toggleAutoCaptureExpenses(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoCaptureExpenses(enabled)
        }
    }

    fun toggleAutoCaptureSms(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoCaptureSms(enabled)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            userPreferences.resetToDefaults()
        }
    }

    class Factory(
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(userPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
