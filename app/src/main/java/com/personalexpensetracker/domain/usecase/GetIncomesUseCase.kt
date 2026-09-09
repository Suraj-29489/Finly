package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe all income entries ordered chronologically descending.
 */
class GetIncomesUseCase(
    private val repository: IncomeRepository
) {

    operator fun invoke(): Flow<List<Income>> {
        return repository.getAllIncomes()
    }
}

