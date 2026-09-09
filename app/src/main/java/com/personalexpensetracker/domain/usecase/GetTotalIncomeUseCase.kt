package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

/**
 * Use case to observe the total accumulated income as [BigDecimal].
 */
class GetTotalIncomeUseCase(
    private val repository: IncomeRepository
) {

    operator fun invoke(): Flow<BigDecimal> {
        return repository.getTotalIncomeInCents().map { cents ->
            cents?.let { BigDecimal.valueOf(it, 2) } ?: BigDecimal.ZERO
        }
    }
}

