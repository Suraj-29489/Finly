package com.personalexpensetracker.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import com.personalexpensetracker.domain.model.UserPreferencesData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementation of [UserPreferences] backed by Android [SharedPreferences].
 * Guarantees persistence across screen transitions, activity recreations, and app restarts.
 */
class UserPreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : UserPreferences {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _preferencesFlow = MutableStateFlow(readPreferences())
    override val preferencesFlow: Flow<UserPreferencesData> = _preferencesFlow.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _preferencesFlow.value = readPreferences()
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun getPreferences(): UserPreferencesData = readPreferences()

    private fun readPreferences(): UserPreferencesData {
        val currencyCode = sharedPreferences.getString(KEY_CURRENCY_CODE, AppCurrency.USD.code)
        val themeModeName = sharedPreferences.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val dateFormatPattern = sharedPreferences.getString(KEY_DATE_FORMAT, AppDateFormat.MONTH_DAY_YEAR.pattern)
        val defaultCategory = sharedPreferences.getString(KEY_DEFAULT_CATEGORY, "Food") ?: "Food"
        val defaultSortOrder = sharedPreferences.getString(KEY_DEFAULT_SORT_ORDER, "NEWEST_FIRST") ?: "NEWEST_FIRST"
        val autoGen = sharedPreferences.getBoolean(KEY_AUTO_GEN_RECURRING, true)
        val autoCapture = sharedPreferences.getBoolean(KEY_AUTO_CAPTURE_EXPENSES, false)
        val autoCaptureSms = sharedPreferences.getBoolean(KEY_AUTO_CAPTURE_SMS, false)

        return UserPreferencesData(
            currency = AppCurrency.fromCode(currencyCode),
            themeMode = AppThemeMode.fromName(themeModeName),
            dateFormat = AppDateFormat.fromPattern(dateFormatPattern),
            defaultCategory = defaultCategory,
            defaultSortOrder = defaultSortOrder,
            autoGenerateRecurring = autoGen,
            autoCaptureExpenses = autoCapture,
            autoCaptureSms = autoCaptureSms
        )
    }

    override suspend fun setCurrency(currency: AppCurrency) {
        sharedPreferences.edit().putString(KEY_CURRENCY_CODE, currency.code).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, themeMode.name).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setDateFormat(dateFormat: AppDateFormat) {
        sharedPreferences.edit().putString(KEY_DATE_FORMAT, dateFormat.pattern).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setDefaultCategory(category: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_CATEGORY, category).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setDefaultSortOrder(sortOrder: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_SORT_ORDER, sortOrder).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setAutoGenerateRecurring(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_GEN_RECURRING, enabled).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setAutoCaptureExpenses(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_CAPTURE_EXPENSES, enabled).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun setAutoCaptureSms(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_CAPTURE_SMS, enabled).commit()
        _preferencesFlow.value = readPreferences()
    }

    override suspend fun resetToDefaults() {
        sharedPreferences.edit().clear().commit()
        _preferencesFlow.value = readPreferences()
    }

    companion object {
        const val PREFS_NAME = "finly_user_preferences"
        const val KEY_CURRENCY_CODE = "pref_currency_code"
        const val KEY_THEME_MODE = "pref_theme_mode"
        const val KEY_DATE_FORMAT = "pref_date_format"
        const val KEY_DEFAULT_CATEGORY = "pref_default_category"
        const val KEY_DEFAULT_SORT_ORDER = "pref_default_sort_order"
        const val KEY_AUTO_GEN_RECURRING = "pref_auto_gen_recurring"
        const val KEY_AUTO_CAPTURE_EXPENSES = "pref_auto_capture_expenses"
        const val KEY_AUTO_CAPTURE_SMS = "pref_auto_capture_sms"
    }
}
