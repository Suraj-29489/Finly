package com.personalexpensetracker.ui.util

import com.personalexpensetracker.domain.model.AppDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Central utility for formatting dates based on user preferences.
 * Separates underlying storage (Instant / LocalDate / ISO strings) from display formatting.
 */
object DateFormatter {

    fun format(
        instant: Instant,
        dateFormat: AppDateFormat = AppDateFormat.MONTH_DAY_YEAR,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val localDate = instant.atZone(zoneId).toLocalDate()
        return format(localDate, dateFormat)
    }

    fun format(
        localDate: LocalDate,
        dateFormat: AppDateFormat = AppDateFormat.MONTH_DAY_YEAR
    ): String {
        val formatter = DateTimeFormatter.ofPattern(dateFormat.pattern, Locale.US)
        return localDate.format(formatter)
    }
}

