package com.personalexpensetracker.domain.usecase

import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe all expenses ordered chronologically descending.
 */
class GetExpensesUseCase(
    private val repository: ExpenseRepository
) {

    operator fun invoke(): Flow<List<Expense>> {
        return repository.getAllExpenses()
    }
}
