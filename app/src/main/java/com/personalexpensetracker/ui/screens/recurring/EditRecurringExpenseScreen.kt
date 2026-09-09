package com.personalexpensetracker.ui.screens.recurring

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.domain.model.RecurrenceFrequency
import com.personalexpensetracker.ui.components.FinlyCard
import com.personalexpensetracker.ui.components.FinlyCategoryIcon
import com.personalexpensetracker.ui.components.FinlyIconButton
import com.personalexpensetracker.ui.components.FinlyPrimaryButton
import com.personalexpensetracker.ui.components.FinlySecondaryButton
import com.personalexpensetracker.ui.theme.FinlyCardShapeSmall
import com.personalexpensetracker.ui.theme.FinlyPillShape
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer
import com.personalexpensetracker.ui.util.DateFormatter
import com.personalexpensetracker.ui.util.currentCurrency
import com.personalexpensetracker.ui.util.currentDateFormat
import java.time.LocalDate

/**
 * Modern Finly Edit Recurring Expense Screen:
 * - Header with back navigation and screen title
 * - Hero amount card with prominent typography
 * - Clean frequency pill selector (Daily, Weekly, Monthly, Yearly)
 * - Category picker with squircle icon badge & quick pills
 * - Title & notes inputs
 * - Modern start & end date selector cards
 * - Dual action buttons: Cancel and Save Changes
 */
@Composable
fun EditRecurringExpenseScreen(
    viewModel: EditRecurringExpenseViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val categoryScrollState = rememberScrollState()
    val frequencyScrollState = rememberScrollState()
    val dateFormat = currentDateFormat

    val startDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.onStartDateChange(LocalDate.of(year, month + 1, dayOfMonth))
        },
        uiState.startDate.year,
        uiState.startDate.monthValue - 1,
        uiState.startDate.dayOfMonth
    )

    val currentEndDate = uiState.endDate ?: uiState.startDate.plusMonths(1)
    val endDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.onEndDateChange(LocalDate.of(year, month + 1, dayOfMonth))
        },
        currentEndDate.year,
        currentEndDate.monthValue - 1,
        currentEndDate.dayOfMonth
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
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
                            text = "RECURRING DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Edit Recurring",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            }

            // Error Message Banner
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ),
                    shape = FinlyCardShapeSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.clearErrorMessage() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. HERO AMOUNT CARD
            FinlyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
            ) {
                Text(
                    text = "RECURRING AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentCurrency.symbol,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.Bold,
                        color = FinlyPurple,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = { viewModel.onAmountChange(it) },
                        placeholder = {
                            Text(
                                text = "0.00",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            cursorColor = FinlyPurple
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. RECURRING DETAILS CARD
            FinlyCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Title / Description Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TITLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.onTitleChange(it) },
                            placeholder = { Text("e.g. Netflix, Apartment Rent, Gym") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FinlyPurple,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = FinlyPurple
                            ),
                            shape = FinlyCardShapeSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Active Status Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (uiState.isActive) "Status: Active" else "Status: Paused",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isActive) "Payments generate automatically" else "Paused, no payments will generate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isActive,
                            onCheckedChange = { viewModel.onIsActiveChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = FinlyPurple
                            )
                        )
                    }

                    // Frequency Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FREQUENCY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(frequencyScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecurrenceFrequency.entries.forEach { freq ->
                                val isSelected = uiState.frequency == freq
                                val label = when (freq) {
                                    RecurrenceFrequency.DAILY -> "Daily"
                                    RecurrenceFrequency.WEEKLY -> "Weekly"
                                    RecurrenceFrequency.MONTHLY -> "Monthly"
                                    RecurrenceFrequency.YEARLY -> "Yearly"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onFrequencyChange(freq) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    shape = FinlyPillShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = FinlyPurpleContainer,
                                        selectedLabelColor = FinlyPurple
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.dp, FinlyPurple.copy(alpha = 0.5f))
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    }
                                )
                            }
                        }
                    }

                    // Category Field & Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FinlyCategoryIcon(
                                category = if (uiState.category.isNotBlank()) uiState.category else "Other",
                                size = 46.dp,
                                iconSize = 24.dp
                            )

                            OutlinedTextField(
                                value = uiState.category,
                                onValueChange = { viewModel.onCategoryChange(it) },
                                placeholder = { Text("e.g. Bills, Entertainment") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FinlyPurple,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    cursorColor = FinlyPurple
                                ),
                                shape = FinlyCardShapeSmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Quick Select Category Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(categoryScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.availableCategories.forEach { category ->
                                val isSelected = uiState.category.equals(category, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onCategoryChange(category) },
                                    label = {
                                        Text(
                                            text = category,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    },
                                    shape = FinlyPillShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = FinlyPurpleContainer,
                                        selectedLabelColor = FinlyPurple
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.dp, FinlyPurple.copy(alpha = 0.5f))
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    }
                                )
                            }
                        }
                    }

                    // Start Date Picker Card
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "START DATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(FinlyCardShapeSmall)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { startDatePickerDialog.show() }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = "Start Date",
                                    tint = FinlyPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = DateFormatter.format(uiState.startDate, dateFormat),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FinlyPurple
                            )
                        }
                    }

                    // Optional End Date Toggle & Picker
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "End Date",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically stop recurrence after a date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = uiState.hasEndDate,
                                onCheckedChange = { viewModel.onHasEndDateToggle(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                    checkedTrackColor = FinlyPurple
                                )
                            )
                        }

                        if (uiState.hasEndDate && uiState.endDate != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(FinlyCardShapeSmall)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { endDatePickerDialog.show() }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = "End Date",
                                        tint = FinlyPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = DateFormatter.format(uiState.endDate!!, dateFormat),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FinlyPurple
                                )
                            }
                        }
                    }

                    // Notes Field (Optional)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "NOTES (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = { viewModel.onNotesChange(it) },
                            placeholder = { Text("e.g. Account number, cancellation link, payment method") },
                            singleLine = false,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FinlyPurple,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = FinlyPurple
                            ),
                            shape = FinlyCardShapeSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Cancel & Save Changes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinlySecondaryButton(
                    text = "Cancel",
                    onClick = onNavigateBack,
                    height = 52.dp,
                    modifier = Modifier.weight(1f)
                )

                FinlyPrimaryButton(
                    text = if (uiState.isSubmitting) "Saving..." else "Save Changes",
                    onClick = {
                        viewModel.onSaveChangesClick(onSuccess = onNavigateBack)
                    },
                    enabled = !uiState.isSubmitting,
                    isLoading = uiState.isSubmitting,
                    height = 52.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
