package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository

/**
 * Use case to delete a recurring expense definition.
 * Does NOT delete previously generated historical expense records.
 */
class DeleteRecurringExpenseUseCase(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteRecurringExpenseById(id)
    }

    suspend operator fun invoke(recurringExpense: RecurringExpense) {
        repository.deleteRecurringExpense(recurringExpense)
    }
}

