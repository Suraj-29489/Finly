package com.personalexpensetracker.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.usecase.CreateRecurringExpenseUseCase
import com.personalexpensetracker.domain.validation.RecurringExpenseValidator
import com.personalexpensetracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel managing UI state and operations for creating a new recurring expense.
 */
class AddRecurringExpenseViewModel(
    private val createRecurringExpenseUseCase: CreateRecurringExpenseUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddRecurringExpenseUiState(
            startDate = LocalDate.now(clock.withZone(zoneId))
        )
    )
    val uiState: StateFlow<AddRecurringExpenseUiState> = _uiState.asStateFlow()

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

    fun onNotesChange(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes, errorMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun onSaveRecurringExpense(onSuccess: (() -> Unit)? = null) {
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
                        val recurringExpense = RecurringExpense(
                            title = title,
                            amount = parsedAmount!!,
                            category = category,
                            frequency = currentState.frequency,
                            startDate = currentState.startDate,
                            nextOccurrenceDate = currentState.startDate,
                            isActive = true,
                            endDate = effectiveEndDate,
                            notes = currentState.notes.trim().ifBlank { null }
                        )

                        createRecurringExpenseUseCase(recurringExpense)

                        val today = LocalDate.now(clock.withZone(zoneId))
                        _uiState.value = AddRecurringExpenseUiState(
                            title = "",
                            amount = "",
                            category = Category.DEFAULT,
                            frequency = RecurrenceFrequency.MONTHLY,
                            startDate = today,
                            endDate = null,
                            hasEndDate = false,
                            notes = "",
                            isSubmitting = false,
                            errorMessage = null,
                            successMessage = "Recurring expense added successfully"
                        )
                        onSuccess?.invoke()
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = e.message ?: "Failed to save recurring expense"
                            )
                        }
                    }
                }
            }
        }
    }

    class Factory(
        private val createRecurringExpenseUseCase: CreateRecurringExpenseUseCase,
        private val zoneId: ZoneId = ZoneId.systemDefault(),
        private val clock: Clock = Clock.systemDefaultZone()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddRecurringExpenseViewModel::class.java)) {
                return AddRecurringExpenseViewModel(createRecurringExpenseUseCase, zoneId, clock) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

