package com.personalexpensetracker.ui.screens.expenses

import java.time.LocalDate

/**
 * UI State representing the Edit Expense screen in Phase 2 Step 3.
 */
data class EditExpenseUiState(
    val id: Long = 0L,
    val amount: String = "",
    val category: String = "Food",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isUpdated: Boolean = false
)

