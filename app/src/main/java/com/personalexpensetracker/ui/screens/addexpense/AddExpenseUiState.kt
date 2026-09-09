package com.personalexpensetracker.ui.screens.addexpense

import com.personalexpensetracker.domain.model.Category
import java.time.LocalDate

/**
 * UI state representing the Add Expense screen.
 */
data class AddExpenseUiState(
    val amount: String = "",
    val category: String = Category.DEFAULT,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

