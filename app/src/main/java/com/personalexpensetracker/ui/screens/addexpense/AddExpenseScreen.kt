package com.personalexpensetracker.ui.screens.addexpense

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.ui.components.FinlyCard
import com.personalexpensetracker.ui.components.FinlyCategoryIcon
import com.personalexpensetracker.ui.components.FinlyPrimaryButton
import com.personalexpensetracker.ui.theme.CategoryTheme
import com.personalexpensetracker.ui.theme.FinlyButtonShape
import com.personalexpensetracker.ui.theme.FinlyCardShape
import com.personalexpensetracker.ui.theme.FinlyCardShapeSmall
import com.personalexpensetracker.ui.theme.FinlyIconSquircleShape
import com.personalexpensetracker.ui.theme.FinlyPillShape
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer
import com.personalexpensetracker.ui.theme.StatusOnSuccessContainer
import com.personalexpensetracker.ui.theme.StatusSuccessContainer
import com.personalexpensetracker.ui.util.DateFormatter
import com.personalexpensetracker.ui.util.currentCurrency
import com.personalexpensetracker.ui.util.currentDateFormat
import java.time.LocalDate

/**
 * Modern Finly Add Expense Screen:
 * - Hero amount card with large dominant typography
 * - Category selector with native squircle icon badge & quick pills
 * - Merchant / description input
 * - Date picker card
 * - Prominent purple "Save Expense" button
 */
@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val categoryScrollState = rememberScrollState()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.onDateChange(LocalDate.of(year, month + 1, dayOfMonth))
        },
        uiState.date.year,
        uiState.date.monthValue - 1,
        uiState.date.dayOfMonth
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Message Banner
        if (uiState.successMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = StatusSuccessContainer.copy(alpha = 0.5f)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = StatusOnSuccessContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.successMessage ?: "",
                            color = StatusOnSuccessContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = { viewModel.clearSuccessMessage() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = StatusOnSuccessContainer
                        )
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold)
                    }
                }
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
                        text = uiState.errorMessage ?: "",
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
                text = "AMOUNT",
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

        // 2. TRANSACTION DETAILS CARD
        FinlyCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Category Selection with Squircle Icon
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
                            placeholder = { Text("e.g. Food, Shopping, Transport") },
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
                        AddExpenseViewModel.DEFAULT_CATEGORIES.forEach { category ->
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
                                },
                                shape = FinlyPillShape
                            )
                        }
                    }
                }

                // Description / Merchant Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MERCHANT / DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChange(it) },
                        placeholder = { Text("e.g. Swiggy, Uber, Amazon") },
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

                // Date Selection Card
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DATE",
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
                            .clickable { datePickerDialog.show() }
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
                                contentDescription = "Select Date",
                                tint = FinlyPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = DateFormatter.format(uiState.date, currentDateFormat),
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. SAVE EXPENSE BUTTON
        FinlyPrimaryButton(
            text = if (uiState.isSubmitting) "Saving Expense..." else "Save Expense",
            onClick = { viewModel.onAddExpenseClick() },
            enabled = !uiState.isSubmitting,
            height = 52.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


