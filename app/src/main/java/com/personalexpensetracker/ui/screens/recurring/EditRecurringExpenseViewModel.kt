package com.personalexpensetracker.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.usecase.GetRecurringExpenseByIdUseCase
import com.personalexpensetracker.domain.usecase.UpdateRecurringExpenseUseCase
import com.personalexpensetracker.domain.validation.RecurringExpenseValidator
import com.personalexpensetracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * ViewModel managing UI state and operations for editing an existing recurring expense definition.
 */
class EditRecurringExpenseViewModel(
    private val getRecurringExpenseByIdUseCase: GetRecurringExpenseByIdUseCase,
    private val updateRecurringExpenseUseCase: UpdateRecurringExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRecurringExpenseUiState())
    val uiState: StateFlow<EditRecurringExpenseUiState> = _uiState.asStateFlow()

    private var initialCreatedAt: Instant = Instant.now()

    fun setRecurringExpense(recurringExpense: RecurringExpense) {
        initialCreatedAt = recurringExpense.createdAt
        _uiState.value = EditRecurringExpenseUiState(
            id = recurringExpense.id,
            title = recurringExpense.title,
            amount = recurringExpense.amount.toPlainString(),
            category = recurringExpense.category,
            frequency = recurringExpense.frequency,
            startDate = recurringExpense.startDate,
            nextOccurrenceDate = recurringExpense.nextOccurrenceDate,
            isActive = recurringExpense.isActive,
            endDate = recurringExpense.endDate,
            hasEndDate = recurringExpense.endDate != null,
            notes = recurringExpense.notes ?: "",
            lastGeneratedDate = recurringExpense.lastGeneratedDate,
            isSubmitting = false,
            errorMessage = null,
            isUpdated = false
        )
    }

    fun loadRecurringExpense(id: Long) {
        viewModelScope.launch {
            val expense = getRecurringExpenseByIdUseCase(id)
            if (expense != null) {
                setRecurringExpense(expense)
            } else {
                _uiState.update { it.copy(errorMessage = "Recurring expense not found") }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, errorMessage = null) }
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

    fun onFrequencyChange(newFrequency: RecurrenceFrequency) {
        _uiState.update { it.copy(frequency = newFrequency, errorMessage = null) }
    }

    fun onStartDateChange(newStartDate: LocalDate) {
        _uiState.update { it.copy(startDate = newStartDate, errorMessage = null) }
    }

    fun onEndDateChange(newEndDate: LocalDate?) {
        _uiState.update { it.copy(endDate = newEndDate, errorMessage = null) }
    }

    fun onHasEndDateToggle(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                hasEndDate = enabled,
                endDate = if (enabled) (current.endDate ?: current.startDate.plusMonths(1)) else null,
                errorMessage = null
            )
        }
    }

    fun onIsActiveChange(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive, errorMessage = null) }
    }

    fun onNotesChange(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes, errorMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSaveChangesClick(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val title = currentState.title.trim()
        val category = currentState.category.trim()
        val parsedAmount = try {
            if (currentState.amount.isBlank()) null else BigDecimal(currentState.amount)
        } catch (_: Exception) {
            null
        }

        val effectiveEndDate = if (currentState.hasEndDate) currentState.endDate else null

        val validation = RecurringExpenseValidator.validate(
            title = title,
            amount = parsedAmount,
            category = category,
            startDate = currentState.startDate,
            endDate = effectiveEndDate
        )

        when (validation) {
            is ValidationResult.Invalid -> {
                _uiState.update { it.copy(errorMessage = validation.message) }
                return
            }
            is ValidationResult.Valid -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                    try {
                        val updated = RecurringExpense(
                            id = currentState.id,
                            title = title,
                            amount = parsedAmount!!,
                            category = category,
                            frequency = currentState.frequency,
                            startDate = currentState.startDate,
                            nextOccurrenceDate = currentState.nextOccurrenceDate,
                            isActive = currentState.isActive,
                            endDate = effectiveEndDate,
                            notes = currentState.notes.trim().ifBlank { null },
                            lastGeneratedDate = currentState.lastGeneratedDate,
                            createdAt = initialCreatedAt,
                            updatedAt = Instant.now()
                        )

                        updateRecurringExpenseUseCase(updated)
                        _uiState.update { it.copy(isSubmitting = false, isUpdated = true) }
                        onSuccess()
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = e.message ?: "Failed to update recurring expense"
                            )
                        }
                    }
                }
            }
        }
    }

    class Factory(
        private val getRecurringExpenseByIdUseCase: GetRecurringExpenseByIdUseCase,
        private val updateRecurringExpenseUseCase: UpdateRecurringExpenseUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditRecurringExpenseViewModel::class.java)) {
                return EditRecurringExpenseViewModel(
                    getRecurringExpenseByIdUseCase,
                    updateRecurringExpenseUseCase
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

