package com.personalexpensetracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.export.CsvExporter
import com.personalexpensetracker.data.export.JsonDataExporter
import com.personalexpensetracker.data.preferences.UserPreferencesImpl
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import com.personalexpensetracker.data.repository.IncomeRepositoryImpl
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.IncomeSource
import com.personalexpensetracker.domain.usecase.AddExpenseUseCase
import com.personalexpensetracker.domain.usecase.AddIncomeUseCase
import com.personalexpensetracker.domain.usecase.GetFinancialBalanceUseCase
import com.personalexpensetracker.ui.screens.settings.SettingsViewModel
import com.personalexpensetracker.ui.util.CurrencyFormatter
import com.personalexpensetracker.ui.util.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class Phase10CompleteRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var userPreferences: UserPreferencesImpl
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private lateinit var incomeRepo: IncomeRepositoryImpl
    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var addIncomeUseCase: AddIncomeUseCase
    private lateinit var getFinancialBalanceUseCase: GetFinancialBalanceUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val sharedPrefs = context.getSharedPreferences("phase10_regression_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        userPreferences = UserPreferencesImpl(sharedPrefs)

        expenseRepo = ExpenseRepositoryImpl(database.expenseDao())
        incomeRepo = IncomeRepositoryImpl(database.incomeDao())
        addExpenseUseCase = AddExpenseUseCase(expenseRepo)
        addIncomeUseCase = AddIncomeUseCase(incomeRepo)
        getFinancialBalanceUseCase = GetFinancialBalanceUseCase(expenseRepo, incomeRepo)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun test01_defaultPreferences_allDefaultsCorrect() {
        val prefs = userPreferences.getPreferences()
        assertEquals(AppCurrency.USD, prefs.currency)
        assertEquals(AppThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(AppDateFormat.MONTH_DAY_YEAR, prefs.dateFormat)
        assertEquals("Food", prefs.defaultCategory)
        assertEquals("NEWEST_FIRST", prefs.defaultSortOrder)
        assertTrue(prefs.autoGenerateRecurring)
    }

    @Test
    fun test02_currencySelectionAndPersistence() = runTest {
        val currencies = listOf(
            AppCurrency.EUR,
            AppCurrency.GBP,
            AppCurrency.INR,
            AppCurrency.JPY,
            AppCurrency.CAD,
            AppCurrency.AUD,
            AppCurrency.USD
        )

        for (curr in currencies) {
            userPreferences.setCurrency(curr)
            assertEquals(curr, userPreferences.getPreferences().currency)
            assertEquals(curr, userPreferences.preferencesFlow.first().currency)
        }
    }

    @Test
    fun test03_currencyFormatting_preservesNumericAmount() {
        val originalAmount = BigDecimal("1500.50")
        val negativeAmount = BigDecimal("-250.75")
        val zeroAmount = BigDecimal("0.00")

        // Formatted display values
        assertEquals("$1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.USD))
        assertEquals("€1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.EUR))
        assertEquals("£1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.GBP))
        assertEquals("₹1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.INR))
        assertEquals("¥1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.JPY))
        assertEquals("CA$1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.CAD))
        assertEquals("A$1,500.50", CurrencyFormatter.format(originalAmount, AppCurrency.AUD))

        // Negative amounts
        assertEquals("-$250.75", CurrencyFormatter.format(negativeAmount, AppCurrency.USD))
        assertEquals("-€250.75", CurrencyFormatter.format(negativeAmount, AppCurrency.EUR))

        // Zero amounts
        assertEquals("$0.00", CurrencyFormatter.format(zeroAmount, AppCurrency.USD))

        // Verify underlying BigDecimals were not mutated
        assertEquals(BigDecimal("1500.50"), originalAmount)
        assertEquals(BigDecimal("-250.75"), negativeAmount)
        assertEquals(BigDecimal("0.00"), zeroAmount)
    }

    @Test
    fun test04_currencyFormatting_explicitPositiveSign() {
        val amount = BigDecimal("350.00")
        assertEquals("+$350.00", CurrencyFormatter.format(amount, AppCurrency.USD, explicitPositiveSign = true))
        assertEquals("+€350.00", CurrencyFormatter.format(amount, AppCurrency.EUR, explicitPositiveSign = true))
        assertEquals("+₹350.00", CurrencyFormatter.format(amount, AppCurrency.INR, explicitPositiveSign = true))

        val negative = BigDecimal("-50.00")
        assertEquals("-$50.00", CurrencyFormatter.format(negative, AppCurrency.USD, explicitPositiveSign = true))
    }

    @Test
    fun test05_themeModeSelectionAndPersistence() = runTest {
        userPreferences.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, userPreferences.getPreferences().themeMode)
        assertEquals(AppThemeMode.LIGHT, userPreferences.preferencesFlow.first().themeMode)

        userPreferences.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, userPreferences.getPreferences().themeMode)
        assertEquals(AppThemeMode.DARK, userPreferences.preferencesFlow.first().themeMode)

        userPreferences.setThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, userPreferences.getPreferences().themeMode)
    }

    @Test
    fun test06_themeModeRestartPersistence() = runTest {
        userPreferences.setThemeMode(AppThemeMode.DARK)

        // Simulate restart
        val sharedPrefs = context.getSharedPreferences("phase10_regression_prefs", Context.MODE_PRIVATE)
        val restarted = UserPreferencesImpl(sharedPrefs)
        assertEquals(AppThemeMode.DARK, restarted.getPreferences().themeMode)
    }

    @Test
    fun test07_dateFormatSelectionAndPersistence() = runTest {
        val formats = listOf(
            AppDateFormat.MONTH_DAY_YEAR,
            AppDateFormat.DAY_MONTH_YEAR,
            AppDateFormat.YEAR_MONTH_DAY,
            AppDateFormat.US_SLASH
        )

        for (fmt in formats) {
            userPreferences.setDateFormat(fmt)
            assertEquals(fmt, userPreferences.getPreferences().dateFormat)
            assertEquals(fmt, userPreferences.preferencesFlow.first().dateFormat)
        }
    }

    @Test
    fun test08_dateFormatFormatting_consistency() {
        val date = LocalDate.of(2026, 9, 8)
        val instant = date.atStartOfDay(ZoneId.of("UTC")).toInstant()

        // MONTH_DAY_YEAR
        assertEquals("Sep 08, 2026", DateFormatter.format(date, AppDateFormat.MONTH_DAY_YEAR))
        assertEquals("Sep 08, 2026", DateFormatter.format(instant, AppDateFormat.MONTH_DAY_YEAR, ZoneId.of("UTC")))

        // DAY_MONTH_YEAR
        assertEquals("08/09/2026", DateFormatter.format(date, AppDateFormat.DAY_MONTH_YEAR))
        assertEquals("08/09/2026", DateFormatter.format(instant, AppDateFormat.DAY_MONTH_YEAR, ZoneId.of("UTC")))

        // YEAR_MONTH_DAY
        assertEquals("2026-09-08", DateFormatter.format(date, AppDateFormat.YEAR_MONTH_DAY))
        assertEquals("2026-09-08", DateFormatter.format(instant, AppDateFormat.YEAR_MONTH_DAY, ZoneId.of("UTC")))

        // US_SLASH
        assertEquals("09/08/2026", DateFormatter.format(date, AppDateFormat.US_SLASH))
        assertEquals("09/08/2026", DateFormatter.format(instant, AppDateFormat.US_SLASH, ZoneId.of("UTC")))
    }

    @Test
    fun test09_defaultSettings_categorySortAndRecurring() = runTest {
        userPreferences.setDefaultCategory(Category.TRANSPORTATION)
        userPreferences.setDefaultSortOrder("AMOUNT_HIGH_TO_LOW")
        userPreferences.setAutoGenerateRecurring(false)

        val prefs = userPreferences.getPreferences()
        assertEquals(Category.TRANSPORTATION, prefs.defaultCategory)
        assertEquals("AMOUNT_HIGH_TO_LOW", prefs.defaultSortOrder)
        assertFalse(prefs.autoGenerateRecurring)

        // Survives restart
        val sharedPrefs = context.getSharedPreferences("phase10_regression_prefs", Context.MODE_PRIVATE)
        val restarted = UserPreferencesImpl(sharedPrefs)
        assertEquals(Category.TRANSPORTATION, restarted.getPreferences().defaultCategory)
        assertEquals("AMOUNT_HIGH_TO_LOW", restarted.getPreferences().defaultSortOrder)
        assertFalse(restarted.getPreferences().autoGenerateRecurring)
    }

    @Test
    fun test10_resetPreferencesToDefaults() = runTest {
        userPreferences.setCurrency(AppCurrency.JPY)
        userPreferences.setThemeMode(AppThemeMode.DARK)
        userPreferences.setDateFormat(AppDateFormat.YEAR_MONTH_DAY)
        userPreferences.setDefaultCategory(Category.ENTERTAINMENT)
        userPreferences.setDefaultSortOrder("OLDEST_FIRST")
        userPreferences.setAutoGenerateRecurring(false)

        userPreferences.resetToDefaults()

        val reset = userPreferences.getPreferences()
        assertEquals(AppCurrency.USD, reset.currency)
        assertEquals(AppThemeMode.SYSTEM, reset.themeMode)
        assertEquals(AppDateFormat.MONTH_DAY_YEAR, reset.dateFormat)
        assertEquals("Food", reset.defaultCategory)
        assertEquals("NEWEST_FIRST", reset.defaultSortOrder)
        assertTrue(reset.autoGenerateRecurring)
    }

    @Test
    fun test11_settingsViewModel_stateUpdatesAndFlow() = runTest {
        val viewModel = SettingsViewModel(userPreferences)

        viewModel.selectCurrency(AppCurrency.GBP)
        advanceUntilIdle()
        assertEquals(AppCurrency.GBP, viewModel.preferencesState.value.currency)

        viewModel.selectThemeMode(AppThemeMode.LIGHT)
        advanceUntilIdle()
        assertEquals(AppThemeMode.LIGHT, viewModel.preferencesState.value.themeMode)

        viewModel.selectDateFormat(AppDateFormat.DAY_MONTH_YEAR)
        advanceUntilIdle()
        assertEquals(AppDateFormat.DAY_MONTH_YEAR, viewModel.preferencesState.value.dateFormat)

        viewModel.selectDefaultCategory(Category.HEALTH)
        advanceUntilIdle()
        assertEquals(Category.HEALTH, viewModel.preferencesState.value.defaultCategory)

        viewModel.selectDefaultSortOrder("AMOUNT_LOW_TO_HIGH")
        advanceUntilIdle()
        assertEquals("AMOUNT_LOW_TO_HIGH", viewModel.preferencesState.value.defaultSortOrder)

        viewModel.toggleAutoGenerateRecurring(false)
        advanceUntilIdle()
        assertFalse(viewModel.preferencesState.value.autoGenerateRecurring)

        viewModel.resetToDefaults()
        advanceUntilIdle()
        assertEquals(AppCurrency.USD, viewModel.preferencesState.value.currency)
        assertEquals(AppThemeMode.SYSTEM, viewModel.preferencesState.value.themeMode)
    }

    @Test
    fun test12_regression_roomDatabasePreserved() = runTest {
        // Insert sample expenses and incomes
        addExpenseUseCase(Expense(title = "Groceries", amount = BigDecimal("120.00"), category = Category.FOOD, date = Instant.now()))
        addExpenseUseCase(Expense(title = "Bus Pass", amount = BigDecimal("45.00"), category = Category.TRANSPORTATION, date = Instant.now()))
        addIncomeUseCase(Income(title = "Salary", amount = BigDecimal("3000.00"), source = IncomeSource.SALARY, date = Instant.now()))

        val balance = getFinancialBalanceUseCase().first()
        assertEquals(BigDecimal("3000.00"), balance.totalIncome)
        assertEquals(BigDecimal("165.00"), balance.totalExpenses)
        assertEquals(BigDecimal("2835.00"), balance.netBalance)
    }

    @Test
    fun test13_regression_financialBalanceUnchangedByCurrencyPreference() = runTest {
        addExpenseUseCase(Expense(title = "Book", amount = BigDecimal("25.00"), category = Category.EDUCATION, date = Instant.now()))
        addIncomeUseCase(Income(title = "Freelance", amount = BigDecimal("500.00"), source = IncomeSource.FREELANCE, date = Instant.now()))

        // Check balance with USD
        userPreferences.setCurrency(AppCurrency.USD)
        val balanceUSD = getFinancialBalanceUseCase().first()
        assertEquals(BigDecimal("475.00"), balanceUSD.netBalance)

        // Change currency preference to INR
        userPreferences.setCurrency(AppCurrency.INR)
        val balanceINR = getFinancialBalanceUseCase().first()
        assertEquals(BigDecimal("475.00"), balanceINR.netBalance)

        // Change currency preference to EUR
        userPreferences.setCurrency(AppCurrency.EUR)
        val balanceEUR = getFinancialBalanceUseCase().first()
        assertEquals(BigDecimal("475.00"), balanceEUR.netBalance)

        // Verify numeric values remain completely identical across currency switches
        assertEquals(balanceUSD.netBalance, balanceINR.netBalance)
        assertEquals(balanceINR.netBalance, balanceEUR.netBalance)
    }

    @Test
    fun test14_regression_csvAndJsonExportStorageUnaffectedByCurrency() = runTest {
        val expense = Expense(title = "Dinner", amount = BigDecimal("85.50"), category = Category.FOOD, date = Instant.parse("2026-09-08T12:00:00Z"))
        val income = Income(title = "Bonus", amount = BigDecimal("1000.00"), source = IncomeSource.INVESTMENT, date = Instant.parse("2026-09-08T12:00:00Z"))

        // Set UI currency to INR
        userPreferences.setCurrency(AppCurrency.INR)

        // CSV export produces raw decimals without currency symbols
        val csv = CsvExporter.exportExpensesToCsv(listOf(expense))
        assertTrue(csv.contains("85.50"))
        assertFalse(csv.contains("₹"))

        // JSON export produces raw standard format
        val json = JsonDataExporter.exportToJson(
            com.personalexpensetracker.domain.datamanagement.FinancialBackupData(
                expenses = listOf(expense),
                incomes = listOf(income)
            )
        )
        assertTrue(json.contains("\"amount\": \"85.50\""))
        assertTrue(json.contains("\"amount\": \"1000.00\""))
        assertFalse(json.contains("₹"))
    }
}
