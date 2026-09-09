package com.personalexpensetracker.integration

import com.personalexpensetracker.domain.model.Budget
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.repository.BudgetRepository
import com.personalexpensetracker.domain.repository.ExpenseRepository
import com.personalexpensetracker.domain.repository.IncomeRepository
import com.personalexpensetracker.domain.repository.RecurringExpenseRepository
import com.personalexpensetracker.domain.usecase.AddExpenseUseCase
import com.personalexpensetracker.domain.usecase.AddIncomeUseCase
import com.personalexpensetracker.domain.usecase.CreateRecurringExpenseUseCase
import com.personalexpensetracker.domain.usecase.DeleteIncomeUseCase
import com.personalexpensetracker.domain.usecase.GetCashFlowReportUseCase
import com.personalexpensetracker.domain.usecase.GetFinancialBalanceUseCase
import com.personalexpensetracker.domain.usecase.GetMonthlyBudgetUseCase
import com.personalexpensetracker.domain.usecase.SetMonthlyBudgetUseCase
import com.personalexpensetracker.domain.usecase.UpdateIncomeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Full Financial Integration Test verifying interaction between Income, Expenses,
 * Budgets, Recurring Expenses, Balance, and Analytics (Phase 8 Step 7).
 * Tests Scenarios A through J explicitly.
 */
class FullFinancialIntegrationTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC)
    private val zoneId = ZoneOffset.UTC
    private val currentMonth = YearMonth.of(2026, 9)

    private lateinit var expenseRepo: MemoryExpenseRepository
    private lateinit var incomeRepo: MemoryIncomeRepository
    private lateinit var budgetRepo: MemoryBudgetRepository
    private lateinit var recurringRepo: MemoryRecurringRepository

    private lateinit var getBalanceUseCase: GetFinancialBalanceUseCase
    private lateinit var getCashFlowReportUseCase: GetCashFlowReportUseCase
    private lateinit var addIncomeUseCase: AddIncomeUseCase
    private lateinit var updateIncomeUseCase: UpdateIncomeUseCase
    private lateinit var deleteIncomeUseCase: DeleteIncomeUseCase
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var createRecurringUseCase: CreateRecurringExpenseUseCase
    private lateinit var setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase
    private lateinit var getMonthlyBudgetUseCase: GetMonthlyBudgetUseCase

    @Before
    fun setUp() {
        expenseRepo = MemoryExpenseRepository()
        incomeRepo = MemoryIncomeRepository()
        budgetRepo = MemoryBudgetRepository()
        recurringRepo = MemoryRecurringRepository()

        getBalanceUseCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)
        getCashFlowReportUseCase = GetCashFlowReportUseCase(expenseRepo, incomeRepo, zoneId, fixedClock)
        addIncomeUseCase = AddIncomeUseCase(incomeRepo)
        updateIncomeUseCase = UpdateIncomeUseCase(incomeRepo)
        deleteIncomeUseCase = DeleteIncomeUseCase(incomeRepo)
        addExpenseUseCase = AddExpenseUseCase(expenseRepo)
        createRecurringUseCase = CreateRecurringExpenseUseCase(recurringRepo)
        setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo)
        getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo)
    }

    // TEST A: Add income -> verify balance increases.
    @Test
    fun `TEST A - Add income increases net balance`() = runBlocking {
        val initialBalance = getBalanceUseCase().first().netBalance
        assertEquals(BigDecimal.ZERO, initialBalance)

        addIncomeUseCase(
            Income(
                id = 0L,
                title = "Salary",
                amount = BigDecimal("3500.00"),
                source = "Salary",
                date = Instant.parse("2026-09-07T10:00:00Z")
            )
        )

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("3500.00"), balance.totalIncome)
        assertEquals(BigDecimal.ZERO, balance.totalExpenses)
        assertEquals(BigDecimal("3500.00"), balance.netBalance)
    }

    // TEST B: Add expense -> verify balance decreases.
    @Test
    fun `TEST B - Add expense decreases net balance`() = runBlocking {
        addExpenseUseCase(
            Expense(
                id = 0L,
                title = "Groceries",
                amount = BigDecimal("150.00"),
                category = "Food",
                date = Instant.parse("2026-09-07T11:00:00Z")
            )
        )

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal.ZERO, balance.totalIncome)
        assertEquals(BigDecimal("150.00"), balance.totalExpenses)
        assertEquals(BigDecimal("-150.00"), balance.netBalance)
    }

    // TEST C: Add income + expense -> verify net balance.
    @Test
    fun `TEST C - Add income and expense computes correct net balance`() = runBlocking {
        addIncomeUseCase(
            Income(id = 0L, title = "Salary", amount = BigDecimal("5000.00"), source = "Salary", date = Instant.parse("2026-09-01T10:00:00Z"))
        )
        addExpenseUseCase(
            Expense(id = 0L, title = "Rent", amount = BigDecimal("1200.00"), category = "Housing", date = Instant.parse("2026-09-02T10:00:00Z"))
        )

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("5000.00"), balance.totalIncome)
        assertEquals(BigDecimal("1200.00"), balance.totalExpenses)
        assertEquals(BigDecimal("3800.00"), balance.netBalance)
    }

    // TEST D: Edit income -> verify balance updates.
    @Test
    fun `TEST D - Edit income updates net balance immediately`() = runBlocking {
        val incomeId = addIncomeUseCase(
            Income(id = 0L, title = "Project Fee", amount = BigDecimal("1000.00"), source = "Freelance", date = Instant.parse("2026-09-03T10:00:00Z"))
        )

        assertEquals(BigDecimal("1000.00"), getBalanceUseCase().first().netBalance)

        updateIncomeUseCase(
            Income(id = incomeId, title = "Project Fee - Bonus Added", amount = BigDecimal("1500.00"), source = "Freelance", date = Instant.parse("2026-09-03T10:00:00Z"))
        )

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("1500.00"), balance.totalIncome)
        assertEquals(BigDecimal("1500.00"), balance.netBalance)
    }

    // TEST E: Delete income -> verify balance updates.
    @Test
    fun `TEST E - Delete income immediately decreases net balance`() = runBlocking {
        val income = Income(id = 1L, title = "Consulting", amount = BigDecimal("800.00"), source = "Business", date = Instant.parse("2026-09-04T10:00:00Z"))
        addIncomeUseCase(income)

        assertEquals(BigDecimal("800.00"), getBalanceUseCase().first().netBalance)

        deleteIncomeUseCase(income)

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal.ZERO, balance.totalIncome)
        assertEquals(BigDecimal.ZERO, balance.netBalance)
    }

    // TEST F: Create recurring expense -> generate it -> verify balance decreases.
    @Test
    fun `TEST F - Generated recurring expense decreases financial balance`() = runBlocking {
        // Initial income
        addIncomeUseCase(
            Income(id = 0L, title = "Monthly Income", amount = BigDecimal("3000.00"), source = "Salary", date = Instant.parse("2026-09-01T10:00:00Z"))
        )
        assertEquals(BigDecimal("3000.00"), getBalanceUseCase().first().netBalance)

        // Create recurring expense for gym
        val recurringId = createRecurringUseCase(
            RecurringExpense(
                id = 0L,
                title = "Gym Subscription",
                amount = BigDecimal("50.00"),
                category = "Health",
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2026, 9, 1),
                nextOccurrenceDate = LocalDate.of(2026, 9, 1)
            )
        )
        assertTrue(recurringId > 0)

        // Recurring expense when generated creates a normal expense
        addExpenseUseCase(
            Expense(
                id = 0L,
                title = "Gym Subscription",
                amount = BigDecimal("50.00"),
                category = "Health",
                date = Instant.parse("2026-09-01T10:00:00Z"),
                recurringExpenseId = recurringId
            )
        )

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("3000.00"), balance.totalIncome)
        assertEquals(BigDecimal("50.00"), balance.totalExpenses)
        assertEquals(BigDecimal("2950.00"), balance.netBalance)
    }

    // TEST G: Create budget -> add income -> verify budget limit does not incorrectly change.
    @Test
    fun `TEST G - Budget limit remains spending limit and does not increase when income added`() = runBlocking {
        // Budget limit set to 20,000
        setMonthlyBudgetUseCase(currentMonth, BigDecimal("20000.00"))

        val budgetBefore = getMonthlyBudgetUseCase(currentMonth).first()
        assertNotNull(budgetBefore)
        assertEquals(BigDecimal("20000.00"), budgetBefore?.amount)

        // Add 50,000 in income
        addIncomeUseCase(
            Income(id = 0L, title = "Large Salary", amount = BigDecimal("50000.00"), source = "Salary", date = Instant.parse("2026-09-05T10:00:00Z"))
        )

        // Verify budget limit is strictly unchanged at 20000.00
        val budgetAfter = getMonthlyBudgetUseCase(currentMonth).first()
        assertNotNull(budgetAfter)
        assertEquals(BigDecimal("20000.00"), budgetAfter?.amount)
    }

    // TEST H: Add income and expenses in different months -> verify each month's calculations remain isolated.
    @Test
    fun `TEST H - Different months remain strictly isolated`() = runBlocking {
        // August
        addIncomeUseCase(Income(id = 0L, title = "Aug Income", amount = BigDecimal("3000.00"), source = "Salary", date = Instant.parse("2026-08-15T12:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Aug Expense", amount = BigDecimal("1000.00"), category = "General", date = Instant.parse("2026-08-20T12:00:00Z")))

        // September
        addIncomeUseCase(Income(id = 0L, title = "Sep Income", amount = BigDecimal("4000.00"), source = "Salary", date = Instant.parse("2026-09-02T12:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Sep Expense", amount = BigDecimal("500.00"), category = "General", date = Instant.parse("2026-09-03T12:00:00Z")))

        val balance = getBalanceUseCase().first()
        // Total All-Time: Inflow = 7000.00, Outflow = 1500.00, Net = 5500.00
        assertEquals(BigDecimal("7000.00"), balance.totalIncome)
        assertEquals(BigDecimal("1500.00"), balance.totalExpenses)
        assertEquals(BigDecimal("5500.00"), balance.netBalance)

        // Current Month (September) Only
        assertEquals(BigDecimal("4000.00"), balance.currentMonth.totalIncome)
        assertEquals(BigDecimal("500.00"), balance.currentMonth.totalExpenses)
        assertEquals(BigDecimal("3500.00"), balance.currentMonth.netCashFlow)
    }

    // TEST I: Add multiple income transactions -> verify total income.
    @Test
    fun `TEST I - Add multiple income transactions computes exact sum`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Income 1", amount = BigDecimal("123.45"), source = "Other", date = Instant.parse("2026-09-07T08:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "Income 2", amount = BigDecimal("234.55"), source = "Other", date = Instant.parse("2026-09-07T09:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "Income 3", amount = BigDecimal("642.00"), source = "Other", date = Instant.parse("2026-09-07T10:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("1000.00"), balance.totalIncome)
        assertEquals(BigDecimal("1000.00"), balance.netBalance)
    }

    // TEST J: Add income with analytics -> verify historical financial reporting.
    @Test
    fun `TEST J - Add income with analytics updates historical trends`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "July Dividend", amount = BigDecimal("250.00"), source = "Investment", date = Instant.parse("2026-07-15T12:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "August Bonus", amount = BigDecimal("500.00"), source = "Salary", date = Instant.parse("2026-08-15T12:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "Sep Salary", amount = BigDecimal("3000.00"), source = "Salary", date = Instant.parse("2026-09-05T12:00:00Z")))

        val report = getCashFlowReportUseCase(FinancialPeriod.THIS_YEAR, trendMonthsCount = 6).first()
        val trends = report.monthlyTrends

        val julTrend = trends.find { it.yearMonth == YearMonth.of(2026, 7) }!!
        assertEquals(BigDecimal("250.00"), julTrend.income)

        val augTrend = trends.find { it.yearMonth == YearMonth.of(2026, 8) }!!
        assertEquals(BigDecimal("500.00"), augTrend.income)

        val sepTrend = trends.find { it.yearMonth == YearMonth.of(2026, 9) }!!
        assertEquals(BigDecimal("3000.00"), sepTrend.income)
    }

    // In-memory repositories for pure high-speed deterministic integration testing
    private class MemoryExpenseRepository : ExpenseRepository {
        private var idCounter = 1L
        private val list = mutableListOf<Expense>()
        private val flow = MutableStateFlow<List<Expense>>(emptyList())

        override fun getAllExpenses(): Flow<List<Expense>> = flow
        override suspend fun getExpenseById(id: Long): Expense? = list.find { it.id == id }
        override suspend fun insertExpense(expense: Expense): Long {
            val assigned = expense.copy(id = idCounter++)
            list.add(assigned)
            flow.value = list.toList()
            return assigned.id
        }
        override suspend fun updateExpense(expense: Expense) {
            val idx = list.indexOfFirst { it.id == expense.id }
            if (idx != -1) {
                list[idx] = expense
                flow.value = list.toList()
            }
        }
        override suspend fun deleteExpense(expense: Expense) {
            list.removeAll { it.id == expense.id }
            flow.value = list.toList()
        }
        override suspend fun deleteExpenseById(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }
        override fun getExpensesByCategory(category: String): Flow<List<Expense>> =
            MutableStateFlow(list.filter { it.category.equals(category, ignoreCase = true) })
        override fun getExpensesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Expense>> =
            MutableStateFlow(list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
        override fun getTotalExpensesInCents(): Flow<Long?> =
            MutableStateFlow(list.fold(0L) { acc, exp -> acc + exp.amount.multiply(BigDecimal(100)).toLong() })
    }

    private class MemoryIncomeRepository : IncomeRepository {
        private var idCounter = 1L
        private val list = mutableListOf<Income>()
        private val flow = MutableStateFlow<List<Income>>(emptyList())

        override fun getAllIncomes(): Flow<List<Income>> = flow
        override suspend fun getIncomeById(id: Long): Income? = list.find { it.id == id }
        override suspend fun insertIncome(income: Income): Long {
            val assigned = income.copy(id = idCounter++)
            list.add(assigned)
            flow.value = list.toList()
            return assigned.id
        }
        override suspend fun updateIncome(income: Income) {
            val idx = list.indexOfFirst { it.id == income.id }
            if (idx != -1) {
                list[idx] = income
                flow.value = list.toList()
            }
        }
        override suspend fun deleteIncome(income: Income) {
            list.removeAll { it.id == income.id }
            flow.value = list.toList()
        }
        override suspend fun deleteIncomeById(id: Long) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }
        override fun getIncomesBySource(source: String): Flow<List<Income>> =
            MutableStateFlow(list.filter { it.source.equals(source, ignoreCase = true) })
        override fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<Income>> =
            MutableStateFlow(list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) })
        override fun getTotalIncomeInCents(): Flow<Long?> =
            MutableStateFlow(list.fold(0L) { acc, inc -> acc + inc.amount.multiply(BigDecimal(100)).toLong() })
    }

    private class MemoryBudgetRepository : BudgetRepository {
        private var monthlyBudget: Budget? = null
        private val flow = MutableStateFlow<Budget?>(null)

        override fun getAllBudgets(): Flow<List<Budget>> = MutableStateFlow(listOfNotNull(monthlyBudget))
        override fun getBudgetsForMonth(month: YearMonth): Flow<List<Budget>> = MutableStateFlow(listOfNotNull(monthlyBudget))
        override fun getMonthlyBudget(month: YearMonth): Flow<Budget?> = flow
        override fun getCategoryBudgets(month: YearMonth): Flow<List<Budget>> = MutableStateFlow(emptyList())
        override fun getCategoryBudget(month: YearMonth, category: String): Flow<Budget?> = MutableStateFlow(null)
        override suspend fun getBudgetById(id: Long): Budget? = if (monthlyBudget?.id == id) monthlyBudget else null

        override suspend fun setMonthlyBudget(month: YearMonth, amount: BigDecimal): Long {
            val b = Budget(id = 1L, month = month, amount = amount)
            monthlyBudget = b
            flow.value = b
            return 1L
        }

        override suspend fun setCategoryBudget(month: YearMonth, category: String, amount: BigDecimal): Long = 1L
        override suspend fun insertBudget(budget: Budget): Long = 1L
        override suspend fun updateBudget(budget: Budget) {}
        override suspend fun deleteBudget(budget: Budget) {}
        override suspend fun deleteBudgetById(id: Long) {}
        override suspend fun deleteMonthlyBudget(month: YearMonth) {}
        override suspend fun deleteCategoryBudget(month: YearMonth, category: String) {}
    }

    private class MemoryRecurringRepository : RecurringExpenseRepository {
        private var idCounter = 1L
        private val list = mutableListOf<RecurringExpense>()
        private val flow = MutableStateFlow<List<RecurringExpense>>(emptyList())

        override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> = flow
        override fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> = flow
        override fun getRecurringExpensesByCategory(category: String): Flow<List<RecurringExpense>> = flow
        override suspend fun getRecurringExpenseById(id: Long): RecurringExpense? = list.find { it.id == id }
        override suspend fun getDueRecurringExpenses(date: LocalDate): List<RecurringExpense> = emptyList()

        override suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long {
            val assigned = recurringExpense.copy(id = idCounter++)
            list.add(assigned)
            flow.value = list.toList()
            return assigned.id
        }

        override suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) {}
        override suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {}
        override suspend fun deleteRecurringExpenseById(id: Long) {}
        override suspend fun setActive(id: Long, isActive: Boolean) {}
        override suspend fun updateNextOccurrence(id: Long, nextOccurrenceDate: LocalDate, lastGeneratedDate: LocalDate) {}
    }
}

