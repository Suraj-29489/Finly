package com.personalexpensetracker.ui.screens.datamanagement

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.repository.DataManagementRepositoryImpl
import com.personalexpensetracker.data.repository.toEntity
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataManagementViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: DataManagementRepositoryImpl
    private lateinit var viewModel: DataManagementViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DataManagementRepositoryImpl(database)
        viewModel = DataManagementViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun exportData_combinedCsv_success() = runTest(testDispatcher) {
        database.expenseDao().insertExpense(Expense(0L, "Lunch", BigDecimal("15.00"), "Food", Instant.now()).toEntity())
        database.incomeDao().insertIncome(Income(0L, "Bonus", BigDecimal("500.00"), "Job", Instant.now()).toEntity())

        viewModel.exportData(ExportType.COMBINED_CSV).join()

        val state = viewModel.uiState.value
        assertNotNull(state.exportedData)
        assertTrue(state.exportedData!!.contains("Lunch"))
        assertTrue(state.exportedData!!.contains("Bonus"))
        assertEquals("finly_combined_ledger.csv", state.exportedFileName)
        assertNotNull(state.successMessage)
    }

    @Test
    fun exportData_json_success() = runTest(testDispatcher) {
        database.expenseDao().insertExpense(Expense(0L, "Coffee", BigDecimal("4.50"), "Food", Instant.now()).toEntity())

        viewModel.exportData(ExportType.JSON_BACKUP).join()

        val state = viewModel.uiState.value
        assertNotNull(state.exportedData)
        assertTrue(state.exportedData!!.contains("\"version\": 1"))
        assertTrue(state.exportedData!!.contains("Coffee"))
        assertEquals("finly_export.json", state.exportedFileName)
    }

    @Test
    fun importCsv_validData_updatesSummary() = runTest(testDispatcher) {
        val csv = """
            Title,Amount,Category,Date,Notes
            Apples,6.20,Food,2026-09-02T10:00:00Z,None
        """.trimIndent()

        viewModel.importCsv(csv).join()

        val state = viewModel.uiState.value
        assertNotNull(state.lastImportSummary)
        assertEquals(1, state.lastImportSummary!!.expensesImported)
        assertNotNull(state.successMessage)
    }

    @Test
    fun createBackup_and_restoreBackup_roundTripSuccess() = runTest(testDispatcher) {
        database.expenseDao().insertExpense(Expense(0L, "Pre-Backup Item", BigDecimal("100.00"), "Tech", Instant.now()).toEntity())

        viewModel.createBackup(password = "securePass123").join()

        val encryptedBackup = viewModel.uiState.value.exportedData
        assertNotNull(encryptedBackup)
        assertTrue(encryptedBackup!!.contains("FINLY_ENCRYPTED_BACKUP"))

        // Wipe DB
        repository.clearAllData()

        // Restore with correct password
        viewModel.restoreBackup(encryptedBackup, "securePass123").join()

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertNull(state.errorMessage)

        val restored = repository.getFullFinancialData()
        assertEquals(1, restored.expenses.size)
        assertEquals("Pre-Backup Item", restored.expenses[0].title)
    }

    @Test
    fun restoreBackup_wrongPassword_setsErrorMessage() = runTest(testDispatcher) {
        database.expenseDao().insertExpense(Expense(0L, "Keep Me", BigDecimal("20.00"), "Food", Instant.now()).toEntity())

        viewModel.createBackup(password = "RealPassword").join()

        val backup = viewModel.uiState.value.exportedData!!

        viewModel.restoreBackup(backup, "WrongPassword").join()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Incorrect password or corrupted backup file"))

        // Data untouched
        val data = repository.getFullFinancialData()
        assertEquals(1, data.expenses.size)
        assertEquals("Keep Me", data.expenses[0].title)
    }
}
