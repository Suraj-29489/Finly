package com.personalexpensetracker.ui.screens.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.usecase.AddExpenseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel managing state and operations for the Add Expense screen.
 */
class AddExpenseViewModel(
    private val addExpenseUseCase: AddExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    fun onAmountChange(newAmount: String) {
        // Allow only valid decimal characters
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

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun onAddExpenseClick() {
        val currentState = _uiState.value

        // Validate description
        val description = currentState.description.trim()
        if (description.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Description cannot be empty") }
            return
        }

        // Validate amount
        val parsedAmount = try {
            BigDecimal(currentState.amount)
        } catch (_: Exception) {
            null
        }

        if (parsedAmount == null || parsedAmount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Amount must be a valid number greater than 0") }
            return
        }

        // Validate category
        val category = currentState.category.trim()
        if (category.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Category cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val expense = Expense(
                    title = description,
                    amount = parsedAmount,
                    category = category,
                    date = currentState.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                )
                addExpenseUseCase(expense)
                // Form reset on success
                _uiState.value = AddExpenseUiState(
                    amount = "",
                    category = category,
                    description = "",
                    date = LocalDate.now(),
                    isSubmitting = false,
                    errorMessage = null,
                    successMessage = "Expense added successfully!"
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to save expense"
                    )
                }
            }
        }
    }

    companion object {
        val DEFAULT_CATEGORIES = Category.BUILT_IN_CATEGORIES
    }

    class Factory(
        private val addExpenseUseCase: AddExpenseUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddExpenseViewModel::class.java)) {
                return AddExpenseViewModel(addExpenseUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

