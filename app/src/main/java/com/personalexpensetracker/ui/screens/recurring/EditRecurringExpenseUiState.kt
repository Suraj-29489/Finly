package com.personalexpensetracker.ui.screens.recurring

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import java.time.LocalDate

/**
 * UI State for the Edit Recurring Expense screen.
 */
data class EditRecurringExpenseUiState(
    val id: Long = 0L,
    val title: String = "",
    val amount: String = "",
    val category: String = Category.DEFAULT,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: LocalDate = LocalDate.now(),
    val nextOccurrenceDate: LocalDate = LocalDate.now(),
    val isActive: Boolean = true,
    val endDate: LocalDate? = null,
    val hasEndDate: Boolean = false,
    val notes: String = "",
    val lastGeneratedDate: LocalDate? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isUpdated: Boolean = false,
    val availableCategories: List<String> = Category.BUILT_IN_CATEGORIES
)

