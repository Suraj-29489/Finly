package com.personalexpensetracker.domain.model

/**
 * Supported currencies for financial amounts display in Finly.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val isSymbolPrefix: Boolean = true
) {
    USD("USD", "$", "USD - US Dollar ($)", true),
    EUR("EUR", "€", "EUR - Euro (€)", true),
    GBP("GBP", "£", "GBP - British Pound (£)", true),
    INR("INR", "₹", "INR - Indian Rupee (₹)", true),
    JPY("JPY", "¥", "JPY - Japanese Yen (¥)", true),
    CAD("CAD", "CA$", "CAD - Canadian Dollar (CA$)", true),
    AUD("AUD", "A$", "AUD - Australian Dollar (A$)", true);

    companion object {
        fun fromCode(code: String?): AppCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}

