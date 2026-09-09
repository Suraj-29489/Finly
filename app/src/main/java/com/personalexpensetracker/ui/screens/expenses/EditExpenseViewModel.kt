package com.personalexpensetracker.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.usecase.UpdateExpenseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel managing state and operations for the Edit Expense screen.
 */
class EditExpenseViewModel(
    private val updateExpenseUseCase: UpdateExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditExpenseUiState())
    val uiState: StateFlow<EditExpenseUiState> = _uiState.asStateFlow()

    private var initialCreatedAt: Instant = Instant.now()

    fun setExpense(expense: Expense) {
        initialCreatedAt = expense.createdAt
        val localDate = expense.date.atZone(ZoneId.systemDefault()).toLocalDate()
        _uiState.value = EditExpenseUiState(
            id = expense.id,
            amount = expense.amount.toPlainString(),
            category = expense.category,
            description = expense.title,
            date = localDate,
            isSubmitting = false,
            errorMessage = null,
            isUpdated = false
        )
    }

    fun onAmountChange(newAmount: String) {
        val sanitized = newAmount.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            _uiState.update { it.copy(amount = sanitized, errorMessage = null) }
        }
    }

    fun onCategoryChange(newCategory: String) {
        _uiState.update { it.copy(category = newCategory, errorMessage = null) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription, errorMessage = null) }
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(date = newDate, errorMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSaveChangesClick(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        val description = currentState.description.trim()
        if (description.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Description cannot be empty") }
            return
        }

        val parsedAmount = try {
            BigDecimal(currentState.amount)
        } catch (_: Exception) {
            null
        }

        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Amount must be a valid number greater than 0") }
            return
        }

        val category = currentState.category.trim()
        if (category.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Category cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val updatedExpense = Expense(
                    id = currentState.id,
                    title = description,
                    amount = parsedAmount,
                    category = category,
                    date = currentState.date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    createdAt = initialCreatedAt
                )
                updateExpenseUseCase(updatedExpense)
                _uiState.update { it.copy(isSubmitting = false, isUpdated = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to update expense"
                    )
                }
            }
        }
    }

    companion object {
        val DEFAULT_CATEGORIES = Category.BUILT_IN_CATEGORIES
    }

    class Factory(
        private val updateExpenseUseCase: UpdateExpenseUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditExpenseViewModel::class.java)) {
                return EditExpenseViewModel(updateExpenseUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

