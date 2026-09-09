package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository

/**
 * Use case to delete an income transaction by entity or ID.
 */
class DeleteIncomeUseCase(
    private val repository: IncomeRepository
) {

    suspend operator fun invoke(income: Income) {
        repository.deleteIncome(income)
    }

    suspend operator fun invoke(id: Long) {
        repository.deleteIncomeById(id)
    }
}

