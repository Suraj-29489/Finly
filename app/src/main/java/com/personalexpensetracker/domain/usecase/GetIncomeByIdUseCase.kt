package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository

/**
 * Use case to retrieve a single income entry by its unique identifier.
 */
class GetIncomeByIdUseCase(
    private val repository: IncomeRepository
) {

    suspend operator fun invoke(id: Long): Income? {
        return repository.getIncomeById(id)
    }
}

