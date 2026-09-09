package com.personalexpensetracker.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import com.personalexpensetracker.ui.util.CurrencyFormatter
import com.personalexpensetracker.ui.util.DateFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class UserPreferencesTest {

    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferencesImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val sharedPrefs = context.getSharedPreferences(UserPreferencesImpl.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        userPreferences = UserPreferencesImpl(sharedPrefs)
    }

    @Test
    fun defaultPreferences_returnsExpectedInitialValues() {
        val prefs = userPreferences.getPreferences()

        assertEquals(AppCurrency.USD, prefs.currency)
        assertEquals(AppThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(AppDateFormat.MONTH_DAY_YEAR, prefs.dateFormat)
        assertEquals("Food", prefs.defaultCategory)
        assertEquals("NEWEST_FIRST", prefs.defaultSortOrder)
        assertTrue(prefs.autoGenerateRecurring)
    }

    @Test
    fun setCurrency_persistsAndUpdatesFlow() = runBlocking {
        userPreferences.setCurrency(AppCurrency.EUR)
        assertEquals(AppCurrency.EUR, userPreferences.getPreferences().currency)
        assertEquals(AppCurrency.EUR, userPreferences.preferencesFlow.first().currency)

        userPreferences.setCurrency(AppCurrency.INR)
        assertEquals(AppCurrency.INR, userPreferences.getPreferences().currency)
        assertEquals(AppCurrency.INR, userPreferences.preferencesFlow.first().currency)

        userPreferences.setCurrency(AppCurrency.GBP)
        assertEquals(AppCurrency.GBP, userPreferences.getPreferences().currency)

        userPreferences.setCurrency(AppCurrency.JPY)
        assertEquals(AppCurrency.JPY, userPreferences.getPreferences().currency)
    }

    @Test
    fun setThemeMode_persistsAndUpdatesFlow() = runBlocking {
        userPreferences.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, userPreferences.getPreferences().themeMode)
        assertEquals(AppThemeMode.DARK, userPreferences.preferencesFlow.first().themeMode)

        userPreferences.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, userPreferences.getPreferences().themeMode)
        assertEquals(AppThemeMode.LIGHT, userPreferences.preferencesFlow.first().themeMode)

        userPreferences.setThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, userPreferences.getPreferences().themeMode)
    }

    @Test
    fun setDateFormat_persistsAndUpdatesFlow() = runBlocking {
        userPreferences.setDateFormat(AppDateFormat.DAY_MONTH_YEAR)
        assertEquals(AppDateFormat.DAY_MONTH_YEAR, userPreferences.getPreferences().dateFormat)
        assertEquals(AppDateFormat.DAY_MONTH_YEAR, userPreferences.preferencesFlow.first().dateFormat)

        userPreferences.setDateFormat(AppDateFormat.YEAR_MONTH_DAY)
        assertEquals(AppDateFormat.YEAR_MONTH_DAY, userPreferences.getPreferences().dateFormat)

        userPreferences.setDateFormat(AppDateFormat.US_SLASH)
        assertEquals(AppDateFormat.US_SLASH, userPreferences.getPreferences().dateFormat)
    }

    @Test
    fun setDefaultCategoryAndSortOrder_persistsCorrectly() = runBlocking {
        userPreferences.setDefaultCategory("Utilities")
        userPreferences.setDefaultSortOrder("AMOUNT_DESC")
        userPreferences.setAutoGenerateRecurring(false)

        val prefs = userPreferences.getPreferences()
        assertEquals("Utilities", prefs.defaultCategory)
        assertEquals("AMOUNT_DESC", prefs.defaultSortOrder)
        assertFalse(prefs.autoGenerateRecurring)
    }

    @Test
    fun preferences_surviveAppRestartSimulation() = runBlocking {
        userPreferences.setCurrency(AppCurrency.INR)
        userPreferences.setThemeMode(AppThemeMode.DARK)
        userPreferences.setDateFormat(AppDateFormat.YEAR_MONTH_DAY)
        userPreferences.setDefaultCategory("Entertainment")
        userPreferences.setAutoGenerateRecurring(false)

        // Simulate app restart / new process by creating a new UserPreferencesImpl instance
        val sharedPrefs = context.getSharedPreferences(UserPreferencesImpl.PREFS_NAME, Context.MODE_PRIVATE)
        val restartedPreferences = UserPreferencesImpl(sharedPrefs)
        val loaded = restartedPreferences.getPreferences()

        assertEquals(AppCurrency.INR, loaded.currency)
        assertEquals(AppThemeMode.DARK, loaded.themeMode)
        assertEquals(AppDateFormat.YEAR_MONTH_DAY, loaded.dateFormat)
        assertEquals("Entertainment", loaded.defaultCategory)
        assertFalse(loaded.autoGenerateRecurring)
    }

    @Test
    fun resetToDefaults_clearsCustomSettings() = runBlocking {
        userPreferences.setCurrency(AppCurrency.GBP)
        userPreferences.setThemeMode(AppThemeMode.LIGHT)
        userPreferences.setDateFormat(AppDateFormat.DAY_MONTH_YEAR)
        userPreferences.setDefaultCategory("Health")

        userPreferences.resetToDefaults()

        val reset = userPreferences.getPreferences()
        assertEquals(AppCurrency.USD, reset.currency)
        assertEquals(AppThemeMode.SYSTEM, reset.themeMode)
        assertEquals(AppDateFormat.MONTH_DAY_YEAR, reset.dateFormat)
        assertEquals("Food", reset.defaultCategory)
        assertTrue(reset.autoGenerateRecurring)
    }

    @Test
    fun currencyFormatter_formatsAmountsCorrectly() {
        assertEquals("$25.00", CurrencyFormatter.format(BigDecimal("25.00"), AppCurrency.USD))
        assertEquals("€1,250.50", CurrencyFormatter.format(BigDecimal("1250.50"), AppCurrency.EUR))
        assertEquals("₹5,000.00", CurrencyFormatter.format(BigDecimal("5000.00"), AppCurrency.INR))
        assertEquals("¥10,000.00", CurrencyFormatter.format(BigDecimal("10000.00"), AppCurrency.JPY))

        // Negative amounts
        assertEquals("-$45.20", CurrencyFormatter.format(BigDecimal("-45.20"), AppCurrency.USD))
        assertEquals("-€100.00", CurrencyFormatter.format(BigDecimal("-100.00"), AppCurrency.EUR))

        // Explicit positive sign
        assertEquals("+$300.00", CurrencyFormatter.format(BigDecimal("300.00"), AppCurrency.USD, explicitPositiveSign = true))
        assertEquals("$0.00", CurrencyFormatter.format(BigDecimal("0.00"), AppCurrency.USD, explicitPositiveSign = true))

        // No symbol
        assertEquals("1,234.56", CurrencyFormatter.format(BigDecimal("1234.56"), AppCurrency.USD, includeSymbol = false))
    }

    @Test
    fun dateFormatter_formatsDatesAccordingToPattern() {
        val testDate = LocalDate.of(2026, 1, 15)

        assertEquals("Jan 15, 2026", DateFormatter.format(testDate, AppDateFormat.MONTH_DAY_YEAR))
        assertEquals("15/01/2026", DateFormatter.format(testDate, AppDateFormat.DAY_MONTH_YEAR))
        assertEquals("2026-01-15", DateFormatter.format(testDate, AppDateFormat.YEAR_MONTH_DAY))
        assertEquals("01/15/2026", DateFormatter.format(testDate, AppDateFormat.US_SLASH))

        // Instant formatting
        val instant = testDate.atStartOfDay(ZoneId.of("UTC")).toInstant()
        assertEquals("Jan 15, 2026", DateFormatter.format(instant, AppDateFormat.MONTH_DAY_YEAR, ZoneId.of("UTC")))
    }
}

