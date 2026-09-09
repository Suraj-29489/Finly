package com.personalexpensetracker.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.usecase.DeleteExpenseUseCase
import com.personalexpensetracker.domain.usecase.GetExpensesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * ViewModel responsible for observing expenses and managing search, filter, sort, and deletion in Phase 2.
 */
class ExpenseListViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow(ExpenseDateFilter.ALL)
    val selectedDateFilter: StateFlow<ExpenseDateFilter> = _selectedDateFilter.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(ExpenseSortOrder.NEWEST_FIRST)
    val selectedSortOrder: StateFlow<ExpenseSortOrder> = _selectedSortOrder.asStateFlow()

    val availableCategories: StateFlow<List<String>> = getExpensesUseCase()
        .map { expenses ->
            val existing = expenses.map { it.category }.distinct()
            (DEFAULT_CATEGORIES + existing).distinct()
        }
        .catch { emit(DEFAULT_CATEGORIES) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DEFAULT_CATEGORIES
        )

    val hasActiveFilters: StateFlow<Boolean> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedDateFilter,
        _selectedSortOrder
    ) { query, category, dateFilter, sortOrder ->
        query.isNotBlank() ||
            category != "All" ||
            dateFilter != ExpenseDateFilter.ALL ||
            sortOrder != ExpenseSortOrder.NEWEST_FIRST
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val uiState: StateFlow<ExpenseListUiState> = combine(
        getExpensesUseCase(),
        _searchQuery,
        _selectedCategory,
        _selectedDateFilter,
        _selectedSortOrder
    ) { expenses, query, category, dateFilter, sortOrder ->
        if (expenses.isEmpty()) {
            ExpenseListUiState.Empty
        } else {
            var filtered = expenses

            // 1. Search by description/title or category or notes
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                filtered = filtered.filter { expense ->
                    expense.title.lowercase().contains(q) ||
                        expense.category.lowercase().contains(q) ||
                        (expense.notes?.lowercase()?.contains(q) == true)
                }
            }

            // 2. Filter by category
            if (category != "All" && category.isNotBlank()) {
                filtered = filtered.filter { expense ->
                    expense.category.equals(category, ignoreCase = true)
                }
            }

            // 3. Filter by date
            when (dateFilter) {
                ExpenseDateFilter.ALL -> {}
                ExpenseDateFilter.TODAY -> {
                    val today = LocalDate.now(clock.withZone(zoneId))
                    filtered = filtered.filter { expense ->
                        expense.date.atZone(zoneId).toLocalDate() == today
                    }
                }
                ExpenseDateFilter.THIS_MONTH -> {
                    val currentMonth = YearMonth.now(clock.withZone(zoneId))
                    filtered = filtered.filter { expense ->
                        YearMonth.from(expense.date.atZone(zoneId)) == currentMonth
                    }
                }
            }

            // Check if results exist after filtering
            if (filtered.isEmpty()) {
                ExpenseListUiState.NoMatchingResults
            } else {
                val sorted = when (sortOrder) {
                    ExpenseSortOrder.NEWEST_FIRST -> filtered.sortedWith(
                        compareByDescending<Expense> { it.date }.thenByDescending { it.id }
                    )
                    ExpenseSortOrder.OLDEST_FIRST -> filtered.sortedWith(
                        compareBy<Expense> { it.date }.thenBy { it.id }
                    )
                    ExpenseSortOrder.AMOUNT_HIGH_TO_LOW -> filtered.sortedWith(
                        compareByDescending<Expense> { it.amount }.thenByDescending { it.date }
                    )
                    ExpenseSortOrder.AMOUNT_LOW_TO_HIGH -> filtered.sortedWith(
                        compareBy<Expense> { it.amount }.thenByDescending { it.date }
                    )
                }
                ExpenseListUiState.Success(sorted)
            }
        }
    }
        .catch { throwable ->
            emit(ExpenseListUiState.Error(throwable.message ?: "Failed to load expenses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseListUiState.Loading
        )

    private val _pendingDeleteExpense = MutableStateFlow<Expense?>(null)
    val pendingDeleteExpense: StateFlow<Expense?> = _pendingDeleteExpense.asStateFlow()

    private val _deletionError = MutableStateFlow<String?>(null)
    val deletionError: StateFlow<String?> = _deletionError.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun onDateFilterSelect(filter: ExpenseDateFilter) {
        _selectedDateFilter.value = filter
    }

    fun onSortOrderSelect(sortOrder: ExpenseSortOrder) {
        _selectedSortOrder.value = sortOrder
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        _selectedDateFilter.value = ExpenseDateFilter.ALL
        _selectedSortOrder.value = ExpenseSortOrder.NEWEST_FIRST
    }

    fun requestDelete(expense: Expense) {
        _deletionError.value = null
        _pendingDeleteExpense.value = expense
    }

    fun cancelDelete() {
        _pendingDeleteExpense.value = null
    }

    fun clearDeletionError() {
        _deletionError.value = null
    }

    fun confirmDelete() {
        val target = _pendingDeleteExpense.value ?: return
        _pendingDeleteExpense.value = null
        viewModelScope.launch {
            try {
                deleteExpenseUseCase(target.id)
            } catch (e: Exception) {
                _deletionError.value = e.message ?: "Failed to delete expense"
            }
        }
    }

    companion object {
        val DEFAULT_CATEGORIES = Category.FILTER_CATEGORIES
    }

    class Factory(
        private val getExpensesUseCase: GetExpensesUseCase,
        private val deleteExpenseUseCase: DeleteExpenseUseCase,
        private val zoneId: ZoneId = ZoneId.systemDefault(),
        private val clock: Clock = Clock.systemDefaultZone()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseListViewModel::class.java)) {
                return ExpenseListViewModel(getExpensesUseCase, deleteExpenseUseCase, zoneId, clock) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
