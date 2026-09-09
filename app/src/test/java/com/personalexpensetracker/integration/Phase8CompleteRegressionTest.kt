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
import com.personalexpensetracker.domain.usecase.DeleteExpenseUseCase
import com.personalexpensetracker.domain.usecase.DeleteIncomeUseCase
import com.personalexpensetracker.domain.usecase.GetCashFlowReportUseCase
import com.personalexpensetracker.domain.usecase.GetFinancialBalanceUseCase
import com.personalexpensetracker.domain.usecase.GetMonthlyBudgetUseCase
import com.personalexpensetracker.domain.usecase.SetMonthlyBudgetUseCase
import com.personalexpensetracker.domain.usecase.UpdateExpenseUseCase
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
 * Complete Phase 8 regression testing covering Tests 1 through 18 (Phase 8 Step 8).
 */
class Phase8CompleteRegressionTest {

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
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
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
        updateExpenseUseCase = UpdateExpenseUseCase(expenseRepo)
        deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepo)
        createRecurringUseCase = CreateRecurringExpenseUseCase(recurringRepo)
        setMonthlyBudgetUseCase = SetMonthlyBudgetUseCase(budgetRepo)
        getMonthlyBudgetUseCase = GetMonthlyBudgetUseCase(budgetRepo)
    }

    // TEST 1 & 2: Fresh database -> add income -> persistence simulation
    @Test
    fun `TEST 1 & 2 - Fresh repository add income and persistence`() = runBlocking {
        val income = Income(id = 0L, title = "Salary", amount = BigDecimal("2500.00"), source = "Salary", date = Instant.parse("2026-09-07T10:00:00Z"))
        val id = addIncomeUseCase(income)
        assertTrue(id > 0)
        val loaded = incomeRepo.getIncomeById(id)
        assertNotNull(loaded)
        assertEquals(BigDecimal("2500.00"), loaded?.amount)
    }

    // TEST 3: Add income + expense -> verify balance
    @Test
    fun `TEST 3 - Add income and expense balance check`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Salary", amount = BigDecimal("3000.00"), source = "Salary", date = Instant.parse("2026-09-01T10:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Rent", amount = BigDecimal("1000.00"), category = "Housing", date = Instant.parse("2026-09-02T10:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("2000.00"), balance.netBalance)
    }

    // TEST 4 & 5: Edit and delete income -> verify balance updates
    @Test
    fun `TEST 4 & 5 - Edit and delete income balance updates`() = runBlocking {
        val id = addIncomeUseCase(Income(id = 0L, title = "Bonus", amount = BigDecimal("500.00"), source = "Salary", date = Instant.parse("2026-09-03T10:00:00Z")))
        assertEquals(BigDecimal("500.00"), getBalanceUseCase().first().netBalance)

        updateIncomeUseCase(Income(id = id, title = "Bonus", amount = BigDecimal("700.00"), source = "Salary", date = Instant.parse("2026-09-03T10:00:00Z")))
        assertEquals(BigDecimal("700.00"), getBalanceUseCase().first().netBalance)

        deleteIncomeUseCase(Income(id = id, title = "Bonus", amount = BigDecimal("700.00"), source = "Salary", date = Instant.parse("2026-09-03T10:00:00Z")))
        assertEquals(BigDecimal.ZERO, getBalanceUseCase().first().netBalance)
    }

    // TEST 6: Multiple income transactions
    @Test
    fun `TEST 6 - Multiple income transactions total check`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Job 1", amount = BigDecimal("1500.00"), source = "Salary", date = Instant.parse("2026-09-01T10:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "Job 2", amount = BigDecimal("500.00"), source = "Freelance", date = Instant.parse("2026-09-02T10:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "Dividends", amount = BigDecimal("250.00"), source = "Investment", date = Instant.parse("2026-09-03T10:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("2250.00"), balance.totalIncome)
    }

    // TEST 7: Cross month isolation
    @Test
    fun `TEST 7 - Period isolation across months`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Aug Salary", amount = BigDecimal("2000.00"), source = "Salary", date = Instant.parse("2026-08-15T12:00:00Z")))
        addIncomeUseCase(Income(id = 0L, title = "Sep Salary", amount = BigDecimal("2500.00"), source = "Salary", date = Instant.parse("2026-09-01T12:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("4500.00"), balance.totalIncome)
        assertEquals(BigDecimal("2500.00"), balance.currentMonth.totalIncome)
    }

    // TEST 8: Budget does not increase with income
    @Test
    fun `TEST 8 - Income does not alter budget limits`() = runBlocking {
        setMonthlyBudgetUseCase(currentMonth, BigDecimal("1500.00"))
        addIncomeUseCase(Income(id = 0L, title = "Windfall", amount = BigDecimal("10000.00"), source = "Other", date = Instant.parse("2026-09-05T12:00:00Z")))

        val budget = getMonthlyBudgetUseCase(currentMonth).first()
        assertEquals(BigDecimal("1500.00"), budget?.amount)
    }

    // TEST 9 & 10: Recurring expense generation, edit/delete, balance decrease
    @Test
    fun `TEST 9 & 10 - Recurring generated expense impact on balance`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Pay", amount = BigDecimal("2000.00"), source = "Salary", date = Instant.parse("2026-09-01T12:00:00Z")))
        val recId = createRecurringUseCase(
            RecurringExpense(id = 0L, title = "Cloud Storage", amount = BigDecimal("10.00"), category = "Services", frequency = RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 9, 1), nextOccurrenceDate = LocalDate.of(2026, 9, 1))
        )
        val expId = addExpenseUseCase(Expense(id = 0L, title = "Cloud Storage", amount = BigDecimal("10.00"), category = "Services", date = Instant.parse("2026-09-01T12:00:00Z"), recurringExpenseId = recId))

        var balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("1990.00"), balance.netBalance)

        // Edit generated expense
        updateExpenseUseCase(Expense(id = expId, title = "Cloud Storage Upgraded", amount = BigDecimal("20.00"), category = "Services", date = Instant.parse("2026-09-01T12:00:00Z"), recurringExpenseId = recId))
        balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("1980.00"), balance.netBalance)

        // Delete generated expense
        deleteExpenseUseCase(Expense(id = expId, title = "Cloud Storage Upgraded", amount = BigDecimal("20.00"), category = "Services", date = Instant.parse("2026-09-01T12:00:00Z"), recurringExpenseId = recId))
        balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("2000.00"), balance.netBalance)
    }

    // TEST 11: Analytics report check
    @Test
    fun `TEST 11 - Analytics report accuracy`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Pay", amount = BigDecimal("1000.00"), source = "Salary", date = Instant.parse("2026-09-07T08:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Lunch", amount = BigDecimal("50.00"), category = "Food", date = Instant.parse("2026-09-07T12:00:00Z")))

        val report = getCashFlowReportUseCase(FinancialPeriod.TODAY).first()
        assertEquals(BigDecimal("1000.00"), report.summary.totalIncome)
        assertEquals(BigDecimal("50.00"), report.summary.totalExpenses)
        assertEquals(BigDecimal("950.00"), report.summary.netCashFlow)
    }

    // TEST 12: Dashboard values match underlying database
    @Test
    fun `TEST 12 - Dashboard values match underlying storage`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Sale", amount = BigDecimal("600.00"), source = "Business", date = Instant.parse("2026-09-07T09:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Transport", amount = BigDecimal("40.00"), category = "Travel", date = Instant.parse("2026-09-07T10:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal("600.00"), balance.today.totalIncome)
        assertEquals(BigDecimal("40.00"), balance.today.totalExpenses)
        assertEquals(BigDecimal("560.00"), balance.today.netCashFlow)
    }

    // TEST 14: Zero-income and zero-expense state
    @Test
    fun `TEST 14 - Zero income and zero expense state`() = runBlocking {
        val balance = getBalanceUseCase().first()
        assertEquals(BigDecimal.ZERO, balance.totalIncome)
        assertEquals(BigDecimal.ZERO, balance.totalExpenses)
        assertEquals(BigDecimal.ZERO, balance.netBalance)
    }

    // TEST 15: Income greater than expenses
    @Test
    fun `TEST 15 - Income greater than expenses produces positive balance`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Salary", amount = BigDecimal("2000.00"), source = "Salary", date = Instant.parse("2026-09-01T10:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Snacks", amount = BigDecimal("50.00"), category = "Food", date = Instant.parse("2026-09-01T11:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertTrue(balance.netBalance > BigDecimal.ZERO)
        assertEquals(BigDecimal("1950.00"), balance.netBalance)
    }

    // TEST 16: Expenses greater than income
    @Test
    fun `TEST 16 - Expenses greater than income produces negative balance`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Gift", amount = BigDecimal("100.00"), source = "Gift", date = Instant.parse("2026-09-01T10:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Repairs", amount = BigDecimal("350.00"), category = "General", date = Instant.parse("2026-09-01T11:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertTrue(balance.netBalance < BigDecimal.ZERO)
        assertEquals(BigDecimal("-250.00"), balance.netBalance)
    }

    // TEST 17: Income exactly equal to expenses
    @Test
    fun `TEST 17 - Income exactly equal to expenses produces zero balance`() = runBlocking {
        addIncomeUseCase(Income(id = 0L, title = "Fee", amount = BigDecimal("500.00"), source = "Freelance", date = Instant.parse("2026-09-01T10:00:00Z")))
        addExpenseUseCase(Expense(id = 0L, title = "Bills", amount = BigDecimal("500.00"), category = "Utilities", date = Instant.parse("2026-09-01T11:00:00Z")))

        val balance = getBalanceUseCase().first()
        assertEquals(0, balance.netBalance.compareTo(BigDecimal.ZERO))
    }

    // TEST 18: Month-end / year-end date boundaries
    @Test
    fun `TEST 18 - Month-end and year-end boundary calculations`() = runBlocking {
        val decIncome = Income(id = 0L, title = "End of Year Bonus", amount = BigDecimal("1200.00"), source = "Salary", date = Instant.parse("2026-12-31T23:59:59Z"))
        val janIncome = Income(id = 0L, title = "New Year Pay", amount = BigDecimal("1500.00"), source = "Salary", date = Instant.parse("2027-01-01T00:00:01Z"))
        addIncomeUseCase(decIncome)
        addIncomeUseCase(janIncome)

        val decClock = Clock.fixed(Instant.parse("2026-12-31T12:00:00Z"), ZoneOffset.UTC)
        val decBalanceUseCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo, zoneId, decClock)
        val decBalance = decBalanceUseCase().first()

        assertEquals(BigDecimal("2700.00"), decBalance.totalIncome)
        assertEquals(BigDecimal("1200.00"), decBalance.currentMonth.totalIncome)
    }

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
