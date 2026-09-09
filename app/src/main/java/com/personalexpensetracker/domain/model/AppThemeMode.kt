package com.personalexpensetracker.domain.model

/**
 * Supported theme modes for Finly application appearance.
 */
enum class AppThemeMode(val displayName: String) {
    SYSTEM("Follow System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): AppThemeMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}

