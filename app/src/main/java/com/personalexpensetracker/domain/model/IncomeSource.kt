package com.personalexpensetracker.domain.model

/**
 * Domain representation of an Income Source / Category in Finly.
 *
 * Serves as the single centralized source of truth for built-in income sources,
 * standardized naming, validation, and filter listings across the application.
 *
 * Built-in sources:
 * 1. Salary
 * 2. Freelance
 * 3. Business
 * 4. Investment
 * 5. Gift
 * 6. Other
 */
data class IncomeSource(
    val name: String,
    val isCustom: Boolean = false
) {
    companion object {
        const val ALL = "All"
        const val SALARY = "Salary"
        const val FREELANCE = "Freelance"
        const val BUSINESS = "Business"
        const val INVESTMENT = "Investment"
        const val GIFT = "Gift"
        const val OTHER = "Other"

        /**
         * Default source assigned to newly created income entries.
         */
        const val DEFAULT = SALARY

        /**
         * Centralized source of truth for all built-in income sources in Finly.
         */
        val BUILT_IN_SOURCES: List<String> = listOf(
            SALARY,
            FREELANCE,
            BUSINESS,
            INVESTMENT,
            GIFT,
            OTHER
        )

        /**
         * Source filter list including the default "All" filter followed by all built-in sources.
         */
        val FILTER_SOURCES: List<String> = listOf(ALL) + BUILT_IN_SOURCES

        /**
         * Checks whether a given source string matches any built-in source (case-insensitive, trimmed).
         */
        fun isBuiltIn(sourceName: String): Boolean {
            return BUILT_IN_SOURCES.any { it.equals(sourceName.trim(), ignoreCase = true) }
        }

        /**
         * Returns the canonical capitalized name for a built-in source if matched,
         * or trims and returns the input string if custom or unrecognized.
         */
        fun normalize(sourceName: String): String {
            val trimmed = sourceName.trim()
            return BUILT_IN_SOURCES.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
        }
    }
}

