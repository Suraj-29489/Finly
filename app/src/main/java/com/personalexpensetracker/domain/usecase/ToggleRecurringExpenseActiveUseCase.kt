package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.repository.RecurringExpenseRepository

/**
 * Use case to pause or resume a recurring expense definition.
 */
class ToggleRecurringExpenseActiveUseCase(
    private val repository: RecurringExpenseRepository
) {
    suspend operator fun invoke(id: Long, isActive: Boolean) {
        repository.setActive(id, isActive)
    }
}

