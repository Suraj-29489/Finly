package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.repository.IncomeRepository
import com.personalexpensetracker.domain.validation.IncomeValidator
import com.personalexpensetracker.domain.validation.ValidationResult

/**
 * Use case to validate, normalize, and persist a new income transaction.
 *
 * Throws [IllegalArgumentException] if validation fails.
 */
class AddIncomeUseCase(
    private val repository: IncomeRepository
) {

    suspend operator fun invoke(income: Income): Long {
        when (val result = IncomeValidator.validate(income.title, income.amount, income.source, income.date)) {
            is ValidationResult.Invalid -> throw IllegalArgumentException(result.message)
            ValidationResult.Valid -> {
                val normalizedSource = IncomeSource.normalize(income.source)
                return repository.insertIncome(income.copy(source = normalizedSource))
            }
        }
    }
}

