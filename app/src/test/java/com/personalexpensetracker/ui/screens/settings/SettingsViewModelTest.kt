package com.personalexpensetracker.ui.screens.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.preferences.UserPreferencesImpl
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferencesImpl
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        val sharedPrefs = context.getSharedPreferences("test_settings_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        userPreferences = UserPreferencesImpl(sharedPrefs)
        viewModel = SettingsViewModel(userPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_matchesDefaultPreferences() {
        val state = viewModel.preferencesState.value
        assertEquals(AppCurrency.USD, state.currency)
        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertEquals(AppDateFormat.MONTH_DAY_YEAR, state.dateFormat)
        assertEquals("Food", state.defaultCategory)
        assertTrue(state.autoGenerateRecurring)
    }

    @Test
    fun selectCurrency_updatesPreferencesAndState() = runTest {
        viewModel.selectCurrency(AppCurrency.EUR)
        advanceUntilIdle()

        assertEquals(AppCurrency.EUR, viewModel.preferencesState.value.currency)
        assertEquals(AppCurrency.EUR, userPreferences.getPreferences().currency)

        viewModel.selectCurrency(AppCurrency.INR)
        advanceUntilIdle()

        assertEquals(AppCurrency.INR, viewModel.preferencesState.value.currency)
        assertEquals(AppCurrency.INR, userPreferences.getPreferences().currency)
    }

    @Test
    fun selectThemeMode_updatesPreferencesAndState() = runTest {
        viewModel.selectThemeMode(AppThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, viewModel.preferencesState.value.themeMode)
        assertEquals(AppThemeMode.DARK, userPreferences.getPreferences().themeMode)
    }

    @Test
    fun selectDateFormat_updatesPreferencesAndState() = runTest {
        viewModel.selectDateFormat(AppDateFormat.DAY_MONTH_YEAR)
        advanceUntilIdle()

        assertEquals(AppDateFormat.DAY_MONTH_YEAR, viewModel.preferencesState.value.dateFormat)
        assertEquals(AppDateFormat.DAY_MONTH_YEAR, userPreferences.getPreferences().dateFormat)
    }

    @Test
    fun selectDefaults_updatesPreferencesAndState() = runTest {
        viewModel.selectDefaultCategory("Transport")
        viewModel.selectDefaultSortOrder("AMOUNT_DESC")
        viewModel.toggleAutoGenerateRecurring(false)
        advanceUntilIdle()

        assertEquals("Transport", viewModel.preferencesState.value.defaultCategory)
        assertEquals("AMOUNT_DESC", viewModel.preferencesState.value.defaultSortOrder)
        assertFalse(viewModel.preferencesState.value.autoGenerateRecurring)
    }

    @Test
    fun resetToDefaults_restoresDefaultState() = runTest {
        viewModel.selectCurrency(AppCurrency.JPY)
        viewModel.selectThemeMode(AppThemeMode.LIGHT)
        advanceUntilIdle()

        viewModel.resetToDefaults()
        advanceUntilIdle()

        assertEquals(AppCurrency.USD, viewModel.preferencesState.value.currency)
        assertEquals(AppThemeMode.SYSTEM, viewModel.preferencesState.value.themeMode)
    }

    @Test
    fun toggleAutoCaptureSms_updatesPreferencesAndState() = runTest {
        assertFalse(viewModel.preferencesState.value.autoCaptureSms)

        viewModel.toggleAutoCaptureSms(true)
        advanceUntilIdle()

        assertTrue(viewModel.preferencesState.value.autoCaptureSms)
        assertTrue(userPreferences.getPreferences().autoCaptureSms)

        viewModel.toggleAutoCaptureSms(false)
        advanceUntilIdle()

        assertFalse(viewModel.preferencesState.value.autoCaptureSms)
        assertFalse(userPreferences.getPreferences().autoCaptureSms)
    }
}

