package com.personalexpensetracker.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.UserPreferencesData
import java.math.BigDecimal

/**
 * CompositionLocal providing active user preferences down the Compose hierarchy.
 */
val LocalUserPreferences = compositionLocalOf {
    UserPreferencesData()
}

/**
 * Convenience accessor for active currency in Compose.
 */
val currentCurrency: AppCurrency
    @Composable get() = LocalUserPreferences.current.currency

/**
 * Convenience accessor for active date format in Compose.
 */
val currentDateFormat: AppDateFormat
    @Composable get() = LocalUserPreferences.current.dateFormat

/**
 * Formats a [BigDecimal] amount with currency symbol and decimal formatting according to [AppCurrency].
 */
fun BigDecimal.toCurrencyString(
    currency: AppCurrency = AppCurrency.USD,
    includeSymbol: Boolean = true,
    explicitPositiveSign: Boolean = false
): String = CurrencyFormatter.format(this, currency, includeSymbol, explicitPositiveSign)

