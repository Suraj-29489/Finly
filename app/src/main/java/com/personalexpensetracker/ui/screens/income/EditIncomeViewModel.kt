package com.personalexpensetracker.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.usecase.GetIncomeByIdUseCase
import com.personalexpensetracker.domain.usecase.UpdateIncomeUseCase
import com.personalexpensetracker.domain.validation.IncomeValidator
import com.personalexpensetracker.domain.validation.ValidationResult
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
 * ViewModel managing state and operations for the Edit Income screen.
 */
class EditIncomeViewModel(
    private val getIncomeByIdUseCase: GetIncomeByIdUseCase,
    private val updateIncomeUseCase: UpdateIncomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditIncomeUiState())
    val uiState: StateFlow<EditIncomeUiState> = _uiState.asStateFlow()

    private var initialCreatedAt: Instant = Instant.now()

    fun setIncome(income: Income) {
        initialCreatedAt = income.createdAt
        val localDate = income.date.atZone(ZoneId.systemDefault()).toLocalDate()
        _uiState.value = EditIncomeUiState(
            id = income.id,
            title = income.title,
            amount = income.amount.toPlainString(),
            source = income.source,
            date = localDate,
            notes = income.notes ?: "",
            isSubmitting = false,
            errorMessage = null,
            isUpdated = false
        )
    }

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            try {
                val income = getIncomeByIdUseCase(id)
                if (income != null) {
                    setIncome(income)
                } else {
                    _uiState.update { it.copy(errorMessage = "Income transaction not found") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to load income")
                }
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

    fun onSaveChangesClick(onSuccess: () -> Unit) {
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
                val updatedIncome = Income(
                    id = currentState.id,
                    title = title,
                    amount = parsedAmount!!,
                    source = IncomeSource.normalize(source),
                    date = dateInstant,
                    notes = notes,
                    createdAt = initialCreatedAt
                )
                updateIncomeUseCase(updatedIncome)
                _uiState.update { it.copy(isSubmitting = false, isUpdated = true) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage ?: "Failed to update income"
                    )
                }
            }
        }
    }

    class Factory(
        private val getIncomeByIdUseCase: GetIncomeByIdUseCase,
        private val updateIncomeUseCase: UpdateIncomeUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EditIncomeViewModel::class.java)) {
                return EditIncomeViewModel(getIncomeByIdUseCase, updateIncomeUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

