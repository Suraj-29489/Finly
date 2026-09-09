package com.personalexpensetracker.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.usecase.AddIncomeUseCase
import com.personalexpensetracker.domain.validation.IncomeValidator
import com.personalexpensetracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel managing state and operations for the Add Income screen.
 */
class AddIncomeViewModel(
    private val addIncomeUseCase: AddIncomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddIncomeUiState())
    val uiState: StateFlow<AddIncomeUiState> = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, errorMessage = null) }
    }

    fun onAmountChange(newAmount: String) {
        val sanitized = newAmount.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            _uiState.update { it.copy(amount = sanitized, errorMessage = null) }
        }
    }

    fun onSourceChange(newSource: String) {
        _uiState.update { it.copy(source = newSource, errorMessage = null) }
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.update { it.copy(date = newDate, errorMessage = null) }
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

    fun onAddIncomeClick() {
        val currentState = _uiState.value

        val title = currentState.title.trim()
        val source = currentState.source.trim()
        val notes = currentState.notes.trim().ifEmpty { null }

        val parsedAmount = try {
            BigDecimal(currentState.amount)
        } catch (_: Exception) {
            null
        }

        val dateInstant = currentState.date.atStartOfDay(ZoneId.systemDefault()).toInstant()

        when (val validation = IncomeValidator.validate(title, parsedAmount, source, dateInstant)) {
            is ValidationResult.Invalid -> {
                _uiState.update { it.copy(errorMessage = validation.message) }
                return
            }
            ValidationResult.Valid -> {
                // validation passed
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val income = Income(
                    title = title,
                    amount = parsedAmount!!,
                    source = source,
                    date = dateInstant,
                    notes = notes
                )
                val id = addIncomeUseCase(income)
                val savedIncome = income.copy(id = id, source = IncomeSource.normalize(source))
                _uiState.value = AddIncomeUiState(
                    title = "",
                    amount = "",
                    source = source,
                    date = LocalDate.now(),
                    notes = "",
                    isSubmitting = false,
                    errorMessage = null,
                    successMessage = "Income \"$title\" recorded successfully!",
                    lastInsertedIncome = savedIncome
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage ?: "Failed to save income"
                    )
                }
            }
        }
    }

    class Factory(
        private val addIncomeUseCase: AddIncomeUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddIncomeViewModel::class.java)) {
                return AddIncomeViewModel(addIncomeUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

