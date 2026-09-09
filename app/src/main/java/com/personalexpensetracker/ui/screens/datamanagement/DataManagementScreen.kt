package com.personalexpensetracker.ui.screens.datamanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.domain.datamanagement.ImportSummary
import com.personalexpensetracker.ui.theme.StatusError
import com.personalexpensetracker.ui.theme.StatusSuccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Warning
import com.personalexpensetracker.ui.components.FinlyCard
import com.personalexpensetracker.ui.components.FinlyIconButton
import com.personalexpensetracker.ui.theme.FinlyCardShapeSmall
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer

@Composable
fun DataManagementScreen(
    viewModel: DataManagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importType by remember { mutableStateOf(ExportType.COMBINED_CSV) }

    var importPayloadText by remember { mutableStateOf("") }
    var backupPassword by remember { mutableStateOf("") }
    var restorePayloadText by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinlyIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    onClick = onNavigateBack
                )

                Column {
                    Text(
                        text = "DATA TOOLS & BACKUPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status / Feedback Card
                if (uiState.isProcessing) {
                    FinlyCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = FinlyPurple,
                                strokeWidth = 2.5.dp
                            )
                            Text("Processing data operation...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                uiState.successMessage?.let { msg ->
                    FinlyCard(
                        containerColor = StatusSuccess.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.35f))
                    ) {
                        Text(msg, color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }

                uiState.errorMessage?.let { err ->
                    FinlyCard(
                        containerColor = StatusError.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.35f))
                    ) {
                        Text(err, color = StatusError, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Import Summary Card
                uiState.lastImportSummary?.let { summary ->
                    ImportSummaryCard(summary = summary)
                }

                // Section 1: Export
                FinlyCard {
                    Text(
                        text = "Export Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download your expenses and income records in standard CSV or JSON format.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportData(ExportType.COMBINED_CSV) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = FinlyCardShapeSmall,
                            colors = ButtonDefaults.buttonColors(containerColor = FinlyPurple)
                        ) {
                            Text("CSV Ledger", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.exportData(ExportType.JSON_BACKUP) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = FinlyCardShapeSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("JSON Export", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportData(ExportType.EXPENSES_CSV) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = FinlyCardShapeSmall
                        ) {
                            Text("Expenses CSV", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportData(ExportType.INCOMES_CSV) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = FinlyCardShapeSmall
                        ) {
                            Text("Incomes CSV", fontSize = 12.sp)
                        }
                    }
                }

                // Section 2: Import
                FinlyCard {
                    Text(
                        text = "Import Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Import records from CSV or JSON. Duplicate entries are automatically identified and skipped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                importType = ExportType.COMBINED_CSV
                                importPayloadText = ""
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = FinlyCardShapeSmall
                        ) {
                            Text("Import CSV", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                importType = ExportType.JSON_BACKUP
                                importPayloadText = ""
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = FinlyCardShapeSmall
                        ) {
                            Text("Import JSON", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Section 3: Backup & Restore (with safety warnings)
                FinlyCard {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create encrypted, version-controlled full backups (.finlybackup) or restore your database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                backupPassword = ""
                                showCreateBackupDialog = true
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = FinlyCardShapeSmall,
                            colors = ButtonDefaults.buttonColors(containerColor = FinlyPurple)
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Backup", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                restorePayloadText = ""
                                restorePassword = ""
                                showRestoreConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = FinlyCardShapeSmall
                        ) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Exported Payload Viewer (if available)
                uiState.exportedData?.let { data ->
                    FinlyCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.exportedFileName ?: "Export Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.clearFeedback() }) {
                                Text("Dismiss", fontSize = 12.sp, color = FinlyPurple)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (data.length > 500) data.take(500) + "\n... [truncated]" else data,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create Backup (Optional Encryption Password)
    if (showCreateBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateBackupDialog = false },
            title = { Text("Create Backup Bundle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "You can optionally encrypt this backup using a password (AES-256-GCM). If you enter a password, you must remember it to restore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createBackup(backupPassword.ifBlank { null })
                        showCreateBackupDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Restore Data (Warning & Confirmation)
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError) },
            title = { Text("Restore Financial Database") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "WARNING: Restoring will REPLACE all current expenses, incomes, budgets, and recurring expenses with the contents of the backup. This operation is atomic.",
                        color = StatusError,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restorePayloadText,
                        onValueChange = { restorePayloadText = it },
                        label = { Text("Paste .finlybackup contents") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("Password (if encrypted)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    onClick = {
                        viewModel.restoreBackup(restorePayloadText, restorePassword.ifBlank { null })
                        showRestoreConfirmDialog = false
                    }
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Import Data
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(if (importType == ExportType.JSON_BACKUP) "Import JSON" else "Import CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste ${if (importType == ExportType.JSON_BACKUP) "JSON" else "CSV"} data below. Duplicate records will be detected and skipped automatically.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importPayloadText,
                        onValueChange = { importPayloadText = it },
                        label = { Text("Content") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importType == ExportType.JSON_BACKUP) {
                            viewModel.importJson(importPayloadText)
                        } else {
                            viewModel.importCsv(importPayloadText)
                        }
                        showImportDialog = false
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ImportSummaryCard(summary: ImportSummary) {
    FinlyCard {
        Text(
            text = "Operation Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "• Expenses imported: ${summary.expensesImported}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "• Incomes imported: ${summary.incomesImported}",
            style = MaterialTheme.typography.bodySmall
        )
        if (summary.budgetsImported > 0) {
            Text(
                text = "• Budgets imported: ${summary.budgetsImported}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (summary.recurringExpensesImported > 0) {
            Text(
                text = "• Recurring expenses imported: ${summary.recurringExpensesImported}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "• Duplicates skipped: ${summary.skippedRecords}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (summary.failedRecords > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Failed records: ${summary.failedRecords}",
                style = MaterialTheme.typography.bodySmall,
                color = StatusError,
                fontWeight = FontWeight.SemiBold
            )
            summary.errors.take(3).forEach { err ->
                Text(
                    text = "  - $err",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusError
                )
            }
        }
    }
}
