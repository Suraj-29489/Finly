package com.personalexpensetracker.ui.screens.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.personalexpensetracker.data.backup.BackupCryptoManager
import com.personalexpensetracker.data.backup.BackupManager
import com.personalexpensetracker.data.export.CsvExporter
import com.personalexpensetracker.data.export.JsonDataExporter
import com.personalexpensetracker.domain.datamanagement.ImportSummary
import com.personalexpensetracker.domain.repository.DataManagementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExportType {
    EXPENSES_CSV,
    INCOMES_CSV,
    COMBINED_CSV,
    JSON_BACKUP
}

data class DataManagementUiState(
    val isProcessing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastImportSummary: ImportSummary? = null,
    val exportedData: String? = null,
    val exportedFileName: String? = null
)

class DataManagementViewModel(
    private val repository: DataManagementRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    fun exportData(type: ExportType): Job = viewModelScope.launch(ioDispatcher) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null, successMessage = null) }
        try {
            val fullData = repository.getFullFinancialData()
            val content: String
            val fileName: String

            when (type) {
                ExportType.EXPENSES_CSV -> {
                    content = CsvExporter.exportExpensesToCsv(fullData.expenses)
                    fileName = "finly_expenses.csv"
                }
                ExportType.INCOMES_CSV -> {
                    content = CsvExporter.exportIncomesToCsv(fullData.incomes)
                    fileName = "finly_incomes.csv"
                }
                ExportType.COMBINED_CSV -> {
                    content = CsvExporter.exportCombinedToCsv(fullData.expenses, fullData.incomes)
                    fileName = "finly_combined_ledger.csv"
                }
                ExportType.JSON_BACKUP -> {
                    content = JsonDataExporter.exportToJson(fullData)
                    fileName = "finly_export.json"
                }
            }

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    exportedData = content,
                    exportedFileName = fileName,
                    successMessage = "Exported successfully: $fileName"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun importCsv(csvContent: String): Job = viewModelScope.launch(ioDispatcher) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null, successMessage = null) }
        try {
            val summary = repository.importFromCsv(csvContent, preventDuplicates = true)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    lastImportSummary = summary,
                    successMessage = if (summary.failedRecords == 0) "CSV imported successfully" else null,
                    errorMessage = if (summary.failedRecords > 0) "Import completed with ${summary.failedRecords} failure(s)" else null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = "Import failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun importJson(jsonContent: String): Job = viewModelScope.launch(ioDispatcher) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null, successMessage = null) }
        try {
            val summary = repository.importFromJson(jsonContent, preventDuplicates = true)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    lastImportSummary = summary,
                    successMessage = if (summary.failedRecords == 0) "JSON imported successfully" else null,
                    errorMessage = if (summary.failedRecords > 0) "Import completed with ${summary.failedRecords} failure(s)" else null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = "Import failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun createBackup(password: String?): Job = viewModelScope.launch(ioDispatcher) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null, successMessage = null) }
        try {
            val pwdChars = password?.takeIf { it.isNotBlank() }?.toCharArray()
            val backupContent = repository.createBackup(pwdChars)
            val fileName = BackupManager.generateDefaultBackupFileName()

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    exportedData = backupContent,
                    exportedFileName = fileName,
                    successMessage = if (pwdChars != null) "Encrypted backup created successfully" else "Backup bundle created successfully"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    errorMessage = "Backup creation failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun restoreBackup(bundleContent: String, password: String?): Job = viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null, successMessage = null) }
            try {
                val pwdChars = password?.takeIf { it.isNotBlank() }?.toCharArray()
                val summary = repository.restoreFromBackupBundle(bundleContent, pwdChars)

                if (summary.failedRecords > 0) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastImportSummary = summary,
                            errorMessage = summary.errors.firstOrNull() ?: "Restore failed"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            lastImportSummary = summary,
                            successMessage = "Database restored successfully (${summary.totalImported} records)"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Restore failed: ${e.localizedMessage}"
                    )
                }
            }
        }

    fun clearFeedback() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null, lastImportSummary = null, exportedData = null) }
    }

    class Factory(private val repository: DataManagementRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DataManagementViewModel(repository) as T
        }
    }
}
