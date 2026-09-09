package com.personalexpensetracker.ui.util

import com.personalexpensetracker.domain.model.AppCurrency
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility for uniform formatting of financial amounts with currency symbols and regional separators.
 * Does not mutate underlying numeric values.
 */
object CurrencyFormatter {

    fun format(
        amount: BigDecimal,
        currency: AppCurrency = AppCurrency.USD,
        includeSymbol: Boolean = true,
        explicitPositiveSign: Boolean = false
    ): String {
        val isNegative = amount < BigDecimal.ZERO
        val isPositive = amount > BigDecimal.ZERO
        val absAmount = amount.abs()

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.00", symbols)
        val formattedNumber = df.format(absAmount)

        val sign = when {
            isNegative -> "-"
            isPositive && explicitPositiveSign -> "+"
            else -> ""
        }

        return if (includeSymbol) {
            if (currency.isSymbolPrefix) {
                "$sign${currency.symbol}$formattedNumber"
            } else {
                "$sign$formattedNumber ${currency.symbol}"
            }
        } else {
            "$sign$formattedNumber"
        }
    }
}

