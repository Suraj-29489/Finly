package com.personalexpensetracker.ui.screens.income

import com.personalexpensetracker.domain.model.IncomeSource
import java.time.LocalDate

/**
 * UI state representing the Edit Income form.
 */
data class EditIncomeUiState(
    val id: Long = 0L,
    val title: String = "",
    val amount: String = "",
    val source: String = IncomeSource.DEFAULT,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isUpdated: Boolean = false
)

