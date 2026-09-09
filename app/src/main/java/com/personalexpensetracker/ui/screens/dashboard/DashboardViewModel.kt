package com.personalexpensetracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.usecase.GetExpensesUseCase
import com.personalexpensetracker.domain.usecase.GetFinancialBalanceUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * ViewModel for the Finly Dashboard screen.
 *
 * Responsibilities:
 * - Observes recorded expenses reactively through [GetExpensesUseCase].
 * - Observes balance and cash-flow through [GetFinancialBalanceUseCase].
 * - Computes current calendar month spending, today's spending, and transaction counts.
 * - Extracts the top 5 most recent transactions ordered newest-first.
 * - Exposes state as a [StateFlow] of [DashboardUiState].
 */
class DashboardViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getFinancialBalanceUseCase: GetFinancialBalanceUseCase?,
    private val zoneId: ZoneId,
    private val clock: Clock
) : ViewModel() {

    constructor(
        getExpensesUseCase: GetExpensesUseCase,
        zoneId: ZoneId,
        clock: Clock
    ) : this(getExpensesUseCase, null, zoneId, clock)

    constructor(
        getExpensesUseCase: GetExpensesUseCase
    ) : this(getExpensesUseCase, null, ZoneId.systemDefault(), Clock.systemDefaultZone())

    constructor(
        getExpensesUseCase: GetExpensesUseCase,
        getFinancialBalanceUseCase: GetFinancialBalanceUseCase?
    ) : this(getExpensesUseCase, getFinancialBalanceUseCase, ZoneId.systemDefault(), Clock.systemDefaultZone())

    val uiState: StateFlow<DashboardUiState> = if (getFinancialBalanceUseCase != null) {
        combine(
            getExpensesUseCase(),
            getFinancialBalanceUseCase()
        ) { expenses, balance ->
            if (expenses.isEmpty() && balance.totalIncome.compareTo(BigDecimal.ZERO) == 0) {
                DashboardUiState.Empty
            } else {
                val currentMonth = YearMonth.now(clock.withZone(zoneId))
                val today = LocalDate.now(clock.withZone(zoneId))

                val currentMonthExpenses = expenses.filter { expense ->
                    YearMonth.from(expense.date.atZone(zoneId)) == currentMonth
                }
                val currentMonthTotal = currentMonthExpenses.fold(BigDecimal.ZERO.setScale(2)) { acc, exp ->
                    acc.add(exp.amount)
                }

                val todayExpenses = expenses.filter { expense ->
                    expense.date.atZone(zoneId).toLocalDate() == today
                }
                val todayTotal = todayExpenses.fold(BigDecimal.ZERO.setScale(2)) { acc, exp ->
                    acc.add(exp.amount)
                }

                val recent = expenses
                    .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
                    .take(5)

                val topCategories = currentMonthExpenses
                    .groupBy { it.category }
                    .map { (category, list) ->
                        val sum = list.fold(BigDecimal.ZERO.setScale(2)) { acc, exp -> acc.add(exp.amount) }
                        val pct = if (currentMonthTotal.compareTo(BigDecimal.ZERO) > 0) {
                            (sum.toDouble() / currentMonthTotal.toDouble() * 100).toInt()
                        } else 0
                        CategorySpend(category = category, amount = sum, percentage = pct)
                    }
                    .sortedByDescending { it.amount }
                    .take(4)

                DashboardUiState.Success(
                    currentMonthTotal = currentMonthTotal,
                    todayTotal = todayTotal,
                    currentMonthTransactionCount = currentMonthExpenses.size,
                    recentTransactions = recent,
                    netBalance = balance.netBalance,
                    totalIncome = balance.totalIncome,
                    totalExpenses = balance.totalExpenses,
                    currentMonthIncome = balance.currentMonth.totalIncome,
                    currentMonthCashFlow = balance.currentMonth.netCashFlow,
                    todayIncome = balance.today.totalIncome,
                    todayCashFlow = balance.today.netCashFlow,
                    topCategories = topCategories
                )
            }
        }.catch {
            emit(DashboardUiState.Empty)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
    } else {
        getExpensesUseCase()
            .map { expenses ->
                if (expenses.isEmpty()) {
                    DashboardUiState.Empty
                } else {
                    val currentMonth = YearMonth.now(clock.withZone(zoneId))
                    val today = LocalDate.now(clock.withZone(zoneId))

                    val currentMonthExpenses = expenses.filter { expense ->
                        YearMonth.from(expense.date.atZone(zoneId)) == currentMonth
                    }
                    val currentMonthTotal = currentMonthExpenses.fold(BigDecimal.ZERO.setScale(2)) { acc, exp ->
                        acc.add(exp.amount)
                    }

                    val todayExpenses = expenses.filter { expense ->
                        expense.date.atZone(zoneId).toLocalDate() == today
                    }
                    val todayTotal = todayExpenses.fold(BigDecimal.ZERO.setScale(2)) { acc, exp ->
                        acc.add(exp.amount)
                    }

                    val recent = expenses
                        .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
                        .take(5)

                    val topCategories = currentMonthExpenses
                        .groupBy { it.category }
                        .map { (category, list) ->
                            val sum = list.fold(BigDecimal.ZERO.setScale(2)) { acc, exp -> acc.add(exp.amount) }
                            val pct = if (currentMonthTotal.compareTo(BigDecimal.ZERO) > 0) {
                                (sum.toDouble() / currentMonthTotal.toDouble() * 100).toInt()
                            } else 0
                            CategorySpend(category = category, amount = sum, percentage = pct)
                        }
                        .sortedByDescending { it.amount }
                        .take(4)

                    DashboardUiState.Success(
                        currentMonthTotal = currentMonthTotal,
                        todayTotal = todayTotal,
                        currentMonthTransactionCount = currentMonthExpenses.size,
                        recentTransactions = recent,
                        netBalance = currentMonthTotal.negate(),
                        totalIncome = BigDecimal.ZERO,
                        totalExpenses = currentMonthTotal,
                        currentMonthIncome = BigDecimal.ZERO,
                        currentMonthCashFlow = currentMonthTotal.negate(),
                        todayIncome = BigDecimal.ZERO,
                        todayCashFlow = todayTotal.negate(),
                        topCategories = topCategories
                    )
                }
            }
            .catch {
                emit(DashboardUiState.Empty)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DashboardUiState.Loading
            )
    }

    class Factory(
        private val getExpensesUseCase: GetExpensesUseCase,
        private val getFinancialBalanceUseCase: GetFinancialBalanceUseCase?,
        private val zoneId: ZoneId = ZoneId.systemDefault(),
        private val clock: Clock = Clock.systemDefaultZone()
    ) : ViewModelProvider.Factory {

        constructor(
            getExpensesUseCase: GetExpensesUseCase,
            zoneId: ZoneId = ZoneId.systemDefault(),
            clock: Clock = Clock.systemDefaultZone()
        ) : this(getExpensesUseCase, null, zoneId, clock)

        constructor(
            getExpensesUseCase: GetExpensesUseCase
        ) : this(getExpensesUseCase, null, ZoneId.systemDefault(), Clock.systemDefaultZone())

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(
                    getExpensesUseCase,
                    getFinancialBalanceUseCase,
                    zoneId,
                    clock
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
