package com.personalexpensetracker.ui.screens.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.usecase.DeleteIncomeUseCase
import com.personalexpensetracker.domain.usecase.GetIncomesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * ViewModel for viewing, searching, filtering, sorting, and deleting incomes (Phase 8 Step 3).
 */
class IncomeListViewModel(
    private val getIncomesUseCase: GetIncomesUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSource = MutableStateFlow("All")
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow(IncomeDateFilter.ALL)
    val selectedDateFilter: StateFlow<IncomeDateFilter> = _selectedDateFilter.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(IncomeSortOrder.NEWEST_FIRST)
    val selectedSortOrder: StateFlow<IncomeSortOrder> = _selectedSortOrder.asStateFlow()

    val availableSources: StateFlow<List<String>> = getIncomesUseCase()
        .map { incomes ->
            val existing = incomes.map { it.source }.distinct()
            (listOf("All") + IncomeSource.BUILT_IN_SOURCES + existing).distinct()
        }
        .catch { emit(listOf("All") + IncomeSource.BUILT_IN_SOURCES) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("All") + IncomeSource.BUILT_IN_SOURCES
        )

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _searchQuery,
        _selectedSource,
        _selectedDateFilter,
        _selectedSortOrder
    ) { query, source, dateFilter, sortOrder ->
        query.isNotBlank() ||
            source != "All" ||
            dateFilter != IncomeDateFilter.ALL ||
            sortOrder != IncomeSortOrder.NEWEST_FIRST
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val uiState: StateFlow<IncomeListUiState> = combine(
        getIncomesUseCase(),
        _searchQuery,
        _selectedSource,
        _selectedDateFilter,
        _selectedSortOrder
    ) { incomes, query, source, dateFilter, sortOrder ->
        if (incomes.isEmpty()) {
            IncomeListUiState.Empty(isFiltered = false)
        } else {
            var filtered = incomes

            // 1. Search by title or source or notes
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                filtered = filtered.filter { income ->
                    income.title.lowercase().contains(q) ||
                        income.source.lowercase().contains(q) ||
                        (income.notes?.lowercase()?.contains(q) == true)
                }
            }

            // 2. Filter by source
            if (source != "All" && source.isNotBlank()) {
                filtered = filtered.filter { income ->
                    income.source.equals(source, ignoreCase = true)
                }
            }

            // 3. Filter by date
            val today = LocalDate.now(clock)
            filtered = when (dateFilter) {
                IncomeDateFilter.ALL -> filtered
                IncomeDateFilter.TODAY -> {
                    filtered.filter { income ->
                        val incomeDate = income.date.atZone(zoneId).toLocalDate()
                        incomeDate == today
                    }
                }
                IncomeDateFilter.THIS_MONTH -> {
                    val currentYearMonth = YearMonth.from(today)
                    filtered.filter { income ->
                        val incomeDate = income.date.atZone(zoneId).toLocalDate()
                        YearMonth.from(incomeDate) == currentYearMonth
                    }
                }
            }

            // 4. Sort
            val sorted = when (sortOrder) {
                IncomeSortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.date }
                IncomeSortOrder.OLDEST_FIRST -> filtered.sortedBy { it.date }
                IncomeSortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.amount }
                IncomeSortOrder.LOWEST_AMOUNT -> filtered.sortedBy { it.amount }
            }

            if (sorted.isEmpty()) {
                IncomeListUiState.Empty(isFiltered = true)
            } else {
                val totalInflow = sorted.fold(BigDecimal.ZERO) { acc, income -> acc + income.amount }
                IncomeListUiState.Success(
                    incomes = sorted,
                    totalInflow = totalInflow
                )
            }
        }
    }.catch { e ->
        emit(IncomeListUiState.Error(e.localizedMessage ?: "Failed to load income records"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IncomeListUiState.Loading
    )

    private val _pendingDeleteIncome = MutableStateFlow<Income?>(null)
    val pendingDeleteIncome: StateFlow<Income?> = _pendingDeleteIncome.asStateFlow()

    private val _deletionError = MutableStateFlow<String?>(null)
    val deletionError: StateFlow<String?> = _deletionError.asStateFlow()

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onSourceSelect(source: String) {
        _selectedSource.value = source
    }

    fun onDateFilterSelect(filter: IncomeDateFilter) {
        _selectedDateFilter.value = filter
    }

    fun onSortOrderSelect(sortOrder: IncomeSortOrder) {
        _selectedSortOrder.value = sortOrder
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedSource.value = "All"
        _selectedDateFilter.value = IncomeDateFilter.ALL
        _selectedSortOrder.value = IncomeSortOrder.NEWEST_FIRST
    }

    fun onRequestDelete(income: Income) {
        _pendingDeleteIncome.value = income
        _deletionError.value = null
    }

    fun cancelDelete() {
        _pendingDeleteIncome.value = null
        _deletionError.value = null
    }

    fun confirmDelete() {
        val toDelete = _pendingDeleteIncome.value ?: return
        viewModelScope.launch {
            try {
                deleteIncomeUseCase(toDelete.id)
                _pendingDeleteIncome.value = null
            } catch (e: Exception) {
                _deletionError.value = "Failed to delete income: ${e.localizedMessage}"
            }
        }
    }

    fun clearDeletionError() {
        _deletionError.value = null
    }
}

class IncomeListViewModelFactory(
    private val getIncomesUseCase: GetIncomesUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeListViewModel::class.java)) {
            return IncomeListViewModel(
                getIncomesUseCase = getIncomesUseCase,
                deleteIncomeUseCase = deleteIncomeUseCase,
                zoneId = zoneId,
                clock = clock
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

