package com.personalexpensetracker.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.personalexpensetracker.domain.model.AppCurrency
import com.personalexpensetracker.domain.model.AppDateFormat
import com.personalexpensetracker.domain.model.AppThemeMode
import com.personalexpensetracker.domain.model.Category
import com.personalexpensetracker.ui.components.FinlyCard
import com.personalexpensetracker.ui.components.FinlyIconButton
import com.personalexpensetracker.ui.screens.expenses.ExpenseSortOrder
import com.personalexpensetracker.ui.theme.FinlyCardShapeSmall
import com.personalexpensetracker.ui.theme.FinlyIconSquircleShape
import com.personalexpensetracker.ui.theme.FinlyPillShape
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer
import com.personalexpensetracker.ui.theme.StatusError
import com.personalexpensetracker.ui.theme.StatusErrorContainer
import com.personalexpensetracker.ui.theme.StatusSuccess
import com.personalexpensetracker.ui.theme.StatusSuccessContainer

/**
 * Modern Finly Settings Screen:
 * - Grouped fintech cards with soft squircle icon badges
 * - Preference pills with current selections
 * - Automatic expense capture status indicators
 * - Native dialog pickers
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDataManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferencesState.collectAsState()
    val context = LocalContext.current

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showDefaultCategoryDialog by remember { mutableStateOf(false) }
    var showDefaultSortDialog by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    val isNotificationAccessGranted = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)

    var showRestrictedSettingsDialog by remember { mutableStateOf(false) }
    var testSimulationResult by remember { mutableStateOf<String?>(null) }
    var isSimulating by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        text = "PREFERENCES & CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Settings",
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
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Preferences
                SettingsSectionLabel(title = "PREFERENCES")

                FinlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.Payments,
                            iconColor = FinlyPurple,
                            iconBackground = FinlyPurpleContainer,
                            title = "Currency",
                            value = "${preferences.currency.symbol} (${preferences.currency.code})",
                            onClick = { showCurrencyDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsRow(
                            icon = Icons.Outlined.Palette,
                            iconColor = Color(0xFF0EA5E9),
                            iconBackground = Color(0xFFE0F2FE),
                            title = "Theme",
                            value = preferences.themeMode.displayName,
                            onClick = { showThemeDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsRow(
                            icon = Icons.Outlined.CalendarToday,
                            iconColor = Color(0xFF10B981),
                            iconBackground = Color(0xFFD1FAE5),
                            title = "Date Format",
                            value = preferences.dateFormat.pattern,
                            onClick = { showDateFormatDialog = true }
                        )
                    }
                }

                // Section 2: Automatic Expense Capture
                SettingsSectionLabel(title = "AUTOMATIC EXPENSE CAPTURE")

                FinlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column {
                        SettingsSwitchRow(
                            icon = Icons.Outlined.AutoAwesome,
                            iconColor = Color(0xFFF59E0B),
                            iconBackground = Color(0xFFFEF3C7),
                            title = "Auto-Capture from Notifications",
                            subtitle = "Detect bank & UPI debit transactions directly from notifications",
                            checked = preferences.autoCaptureExpenses,
                            onCheckedChange = { viewModel.toggleAutoCaptureExpenses(it) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsPermissionRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Notification Access",
                            subtitle = if (isNotificationAccessGranted) "Listener active for auto-capture" else "Tap to grant Android permission",
                            isGranted = isNotificationAccessGranted,
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(fallbackIntent)
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Restricted Settings Helper Row (Android 13+)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRestrictedSettingsDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(FinlyIconSquircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Trouble enabling on Android 13+?",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "How to unblock 'Restricted setting' in 3 steps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Test Bank Message Simulation row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        isSimulating = true
                                        val testNotif = com.personalexpensetracker.data.notification.model.RawNotificationData(
                                            packageName = "com.google.android.apps.messaging",
                                            title = "VK-HDFCBK",
                                            text = "Your A/c ending 4567 debited for INR 450.00 on 09-Sep-26 towards Swiggy. Ref: UPI112233. Avl Bal: INR 12,000",
                                            subText = null,
                                            bigText = null,
                                            receivedAt = java.time.Instant.now(),
                                            notificationKey = "test_bank_sms_${System.currentTimeMillis()}"
                                        )
                                        val processor = com.personalexpensetracker.data.notification.AutoExpenseCapturePipeline.getProcessor(context)
                                        val res = processor.process(testNotif)
                                        testSimulationResult = when (res) {
                                            is com.personalexpensetracker.data.notification.model.NotificationProcessingResult.ExpenseCreated ->
                                                "✓ Captured ₹${res.parsedTransaction.amount} expense (${res.parsedTransaction.merchant}, ${res.category})"
                                            is com.personalexpensetracker.data.notification.model.NotificationProcessingResult.Duplicate ->
                                                "✓ Already recorded (Duplicate prevented)"
                                            else -> "Result: ${res::class.simpleName}"
                                        }
                                        isSimulating = false
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(FinlyIconSquircleShape)
                                        .background(Color(0xFFE0E7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Test Bank Notification Capture",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isSimulating) "Simulating bank SMS..." else "Simulate VK-HDFCBK ₹450 Swiggy SMS",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (testSimulationResult != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(FinlyCardShapeSmall)
                                    .background(StatusSuccessContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = testSimulationResult ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = StatusSuccess
                                )
                            }
                        }
                    }
                }

                // Section 3: Application Defaults
                SettingsSectionLabel(title = "APPLICATION DEFAULTS")

                FinlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.Category,
                            iconColor = Color(0xFFEC4899),
                            iconBackground = Color(0xFFFCE7F3),
                            title = "Default Category",
                            value = preferences.defaultCategory,
                            onClick = { showDefaultCategoryDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsRow(
                            icon = Icons.AutoMirrored.Outlined.Sort,
                            iconColor = Color(0xFF6366F1),
                            iconBackground = Color(0xFFE0E7FF),
                            title = "Default Sort Order",
                            value = preferences.defaultSortOrder.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            onClick = { showDefaultSortDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsSwitchRow(
                            icon = Icons.Outlined.Refresh,
                            iconColor = Color(0xFF14B8A6),
                            iconBackground = Color(0xFFCCFBF1),
                            title = "Auto-Generate Recurring",
                            subtitle = "Automatically create expenses when recurring schedules are due",
                            checked = preferences.autoGenerateRecurring,
                            onCheckedChange = { viewModel.toggleAutoGenerateRecurring(it) }
                        )
                    }
                }

                // Section 4: Tools & Data
                SettingsSectionLabel(title = "TOOLS & DATA")

                FinlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Outlined.Storage,
                            iconColor = FinlyPurple,
                            iconBackground = FinlyPurpleContainer,
                            title = "Export, Import & Backups",
                            subtitle = "Manage CSV exports, encrypted JSON backups, and restore",
                            onClick = onNavigateToDataManagement
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsRow(
                            icon = Icons.Outlined.Warning,
                            iconColor = StatusError,
                            iconBackground = StatusErrorContainer,
                            title = "Reset Preferences",
                            subtitle = "Restore default settings (data remains unaffected)",
                            titleColor = StatusError,
                            onClick = { showResetConfirmationDialog = true }
                        )
                    }
                }

                // Section 5: About Finly
                FinlyCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(FinlyIconSquircleShape)
                                .background(FinlyPurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = FinlyPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Finly Expense Tracker",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Version 1.0.0 • Offline-first & Private",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialogs
    if (showCurrencyDialog) {
        OptionSelectionDialog(
            title = "Select Currency",
            options = AppCurrency.entries,
            selectedOption = preferences.currency,
            optionLabel = { "${it.code} (${it.symbol}) - ${it.name}" },
            onSelect = {
                viewModel.selectCurrency(it)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showThemeDialog) {
        OptionSelectionDialog(
            title = "Select Theme",
            options = AppThemeMode.entries,
            selectedOption = preferences.themeMode,
            optionLabel = { it.displayName },
            onSelect = {
                viewModel.selectThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showDateFormatDialog) {
        OptionSelectionDialog(
            title = "Select Date Format",
            options = AppDateFormat.entries,
            selectedOption = preferences.dateFormat,
            optionLabel = { "${it.pattern} (${it.sample})" },
            onSelect = {
                viewModel.selectDateFormat(it)
                showDateFormatDialog = false
            },
            onDismiss = { showDateFormatDialog = false }
        )
    }

    if (showDefaultCategoryDialog) {
        OptionSelectionDialog(
            title = "Default Category",
            options = Category.BUILT_IN_CATEGORIES,
            selectedOption = preferences.defaultCategory,
            optionLabel = { it },
            onSelect = {
                viewModel.selectDefaultCategory(it)
                showDefaultCategoryDialog = false
            },
            onDismiss = { showDefaultCategoryDialog = false }
        )
    }

    if (showDefaultSortDialog) {
        OptionSelectionDialog(
            title = "Default Sort Order",
            options = ExpenseSortOrder.entries,
            selectedOption = ExpenseSortOrder.entries.firstOrNull { it.name == preferences.defaultSortOrder }
                ?: ExpenseSortOrder.NEWEST_FIRST,
            optionLabel = { it.displayName },
            onSelect = {
                viewModel.selectDefaultSortOrder(it.name)
                showDefaultSortDialog = false
            },
            onDismiss = { showDefaultSortDialog = false }
        )
    }

    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = { Text("Reset Preferences?", fontWeight = FontWeight.Bold) },
            text = { Text("This will restore default currency, theme, and date formatting. Your saved transactions and database will not be affected.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    shape = FinlyCardShapeSmall
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestrictedSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Allowing Restricted Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "On Android 13, 14 & 15, directly downloaded APKs have special permissions restricted by default with the message: 'Restricted setting: For your security, this setting is currently unavailable.'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To allow it in 3 quick steps:\n1. Tap 'Open App Info' below.\n2. In the top-right corner, tap the 3 dots (⋮).\n3. Tap 'Allow restricted settings' and verify your fingerprint/PIN.\n4. Return here and toggle Notification Access ON!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: Once installed via Google Play Store, this manual step is never required.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedSettingsDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallback = Intent(Settings.ACTION_SETTINGS)
                            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(fallback)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinlyPurple)
                ) {
                    Text("Open App Info", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    value: String? = null,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(FinlyIconSquircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (value != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = FinlyPillShape
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(FinlyIconSquircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FinlyPurple
            )
        )
    }
}

@Composable
private fun SettingsPermissionRow(
    icon: ImageVector,
    title: String = "Notification Access",
    subtitle: String? = null,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val displaySubtitle = subtitle ?: if (isGranted) "Listener active for auto-capture" else "Tap to grant Android permission"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(FinlyIconSquircleShape)
                    .background(if (isGranted) StatusSuccessContainer else StatusErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) StatusSuccess else StatusError,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = if (isGranted) StatusSuccessContainer else StatusErrorContainer,
            shape = FinlyPillShape
        ) {
            Text(
                text = if (isGranted) "Active" else "Required",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) StatusSuccess else StatusError,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    val isSelected = (option == selectedOption)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FinlyCardShapeSmall)
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = FinlyPurple
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) FinlyPurple else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}
