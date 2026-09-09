package com.personalexpensetracker.domain.model

/**
 * Domain representation of an Expense Category in Finly.
 *
 * Serves as the single centralized source of truth for built-in categories,
 * standardized naming, validation, and filter listings across the application.
 *
 * Built-in categories:
 * 1. Food
 * 2. Transportation
 * 3. Shopping
 * 4. Bills
 * 5. Entertainment
 * 6. Health
 * 7. Education
 * 8. Travel
 * 9. Other
 */
data class Category(
    val name: String,
    val isCustom: Boolean = false
) {
    companion object {
        const val ALL = "All"
        const val FOOD = "Food"
        const val TRANSPORTATION = "Transportation"
        const val SHOPPING = "Shopping"
        const val BILLS = "Bills"
        const val ENTERTAINMENT = "Entertainment"
        const val HEALTH = "Health"
        const val EDUCATION = "Education"
        const val TRAVEL = "Travel"
        const val OTHER = "Other"

        /**
         * Default category assigned to newly created expenses.
         */
        const val DEFAULT = FOOD

        /**
         * Centralized source of truth for all built-in categories in Finly.
         */
        val BUILT_IN_CATEGORIES: List<String> = listOf(
            FOOD,
            TRANSPORTATION,
            SHOPPING,
            BILLS,
            ENTERTAINMENT,
            HEALTH,
            EDUCATION,
            TRAVEL,
            OTHER
        )

        /**
         * Category filter list including the default "All" filter followed by all built-in categories.
         */
        val FILTER_CATEGORIES: List<String> = listOf(ALL) + BUILT_IN_CATEGORIES

        /**
         * Checks whether a given category string matches any built-in category (case-insensitive, trimmed).
         */
        fun isBuiltIn(categoryName: String): Boolean {
            return BUILT_IN_CATEGORIES.any { it.equals(categoryName.trim(), ignoreCase = true) }
        }

        /**
         * Returns the canonical capitalized name for a built-in category if matched,
         * or trims and returns the input string if custom or unrecognized.
         */
        fun normalize(categoryName: String): String {
            val trimmed = categoryName.trim()
            return BUILT_IN_CATEGORIES.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
        }
    }
}

