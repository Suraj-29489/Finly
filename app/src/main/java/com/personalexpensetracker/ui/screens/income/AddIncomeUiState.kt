package com.personalexpensetracker.ui.screens.income

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import java.time.LocalDate

/**
 * UI state representing the Add Income form.
 */
data class AddIncomeUiState(
    val title: String = "",
    val amount: String = "",
    val source: String = IncomeSource.DEFAULT,
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastInsertedIncome: Income? = null
)

