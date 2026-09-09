package com.personalexpensetracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.usecase.GetCashFlowReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Cash Flow Analytics & Historical Reporting (Phase 8 Step 6).
 */
class CashFlowAnalyticsViewModel(
    private val getCashFlowReportUseCase: GetCashFlowReportUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(FinancialPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<FinancialPeriod> = _selectedPeriod

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CashFlowAnalyticsUiState> = _selectedPeriod
        .flatMapLatest { period ->
            getCashFlowReportUseCase(period)
                .map { report ->
                    CashFlowAnalyticsUiState(
                        selectedPeriod = period,
                        summary = report.summary,
                        monthlyTrends = report.monthlyTrends,
                        incomeCount = report.incomeTransactionCount,
                        expenseCount = report.expenseTransactionCount,
                        isLoading = false
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CashFlowAnalyticsUiState(isLoading = true)
        )

    fun onPeriodSelected(period: FinancialPeriod) {
        _selectedPeriod.value = period
    }

    class Factory(
        private val getCashFlowReportUseCase: GetCashFlowReportUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CashFlowAnalyticsViewModel::class.java)) {
                return CashFlowAnalyticsViewModel(getCashFlowReportUseCase) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

