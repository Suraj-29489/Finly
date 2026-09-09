package com.personalexpensetracker.ui.screens.recurring

import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import java.time.LocalDate

/**
 * UI State for the Add Recurring Expense screen.
 */
data class AddRecurringExpenseUiState(
    val title: String = "",
    val amount: String = "",
    val category: String = Category.DEFAULT,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val hasEndDate: Boolean = false,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val availableCategories: List<String> = Category.BUILT_IN_CATEGORIES
)

