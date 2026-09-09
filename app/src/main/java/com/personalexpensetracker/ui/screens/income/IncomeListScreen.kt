package com.personalexpensetracker.ui.screens.income

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.personalexpensetracker.ui.components.FinlyEmptyState
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.ui.theme.StatusOnSuccessContainer
import com.personalexpensetracker.ui.theme.StatusSuccess
import com.personalexpensetracker.ui.theme.StatusSuccessContainer
import com.personalexpensetracker.ui.util.CurrencyFormatter
import com.personalexpensetracker.ui.util.DateFormatter
import com.personalexpensetracker.ui.util.currentCurrency
import com.personalexpensetracker.ui.util.currentDateFormat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Screen displaying the income history, search, filtering, and management (Phase 8 Step 3).
 */
@Composable
fun IncomeListScreen(
    viewModel: IncomeListViewModel,
    onIncomeClick: (Income) -> Unit = {},
    onNavigateToAddIncome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingDelete by viewModel.pendingDeleteIncome.collectAsState()
    val deletionError by viewModel.deletionError.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val availableSources by viewModel.availableSources.collectAsState()
    val hasActiveFilters by viewModel.hasActiveFilters.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Deletion confirmation dialog
    if (pendingDelete != null) {
        val income = pendingDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete Income?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${income.title}\" (+ ${formatIncomeAmount(income.amount)})? This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Error banner if deletion failed
            if (deletionError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = deletionError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearDeletionError() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = "Search title, source, or notes...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = StatusSuccess
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StatusSuccess,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Sources Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSources) { source ->
                    val isSelected = selectedSource == source
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onSourceSelect(source) },
                        label = {
                            Text(
                                text = source,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = StatusSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = StatusSuccess
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.6f))
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Date Filters & Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Filter Chips
                IncomeDateFilter.values().forEach { filter ->
                    val isSelected = selectedDateFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onDateFilterSelect(filter) },
                        label = {
                            Text(
                                text = filter.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = StatusSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = StatusSuccess
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.6f))
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Sort Order Selector Dropdown
                Box {
                    AssistChip(
                        onClick = { sortMenuExpanded = true },
                        label = {
                            Text(
                                text = selectedSortOrder.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    )

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        IncomeSortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.displayName,
                                        fontWeight = if (selectedSortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSortOrder == order) StatusSuccess else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    viewModel.onSortOrderSelect(order)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Reset Filters Button
                if (hasActiveFilters) {
                    AssistChip(
                        onClick = { viewModel.clearFilters() },
                        label = { Text("Reset") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset filters",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Main Content Area
            when (val state = uiState) {
                is IncomeListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = StatusSuccess,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                is IncomeListUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FinlyEmptyState(
                            title = if (state.isFiltered) "No matching income records found" else "No income recorded yet",
                            subtitle = if (state.isFiltered) "Try adjusting your search query, source, or date filters." else "Track your salary, freelance earnings, investments, and gifts.",
                            icon = Icons.Outlined.Payments,
                            actionText = if (state.isFiltered) "Clear All Filters" else "+ Record Income",
                            onActionClick = if (state.isFiltered) {
                                { viewModel.clearFilters() }
                            } else {
                                onNavigateToAddIncome
                            }
                        )
                    }
                }

                is IncomeListUiState.Success -> {
                    // Total Inflow Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusSuccessContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Inflow (${state.incomes.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = StatusOnSuccessContainer,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+ ${formatIncomeAmount(state.totalInflow)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.incomes,
                            key = { it.id }
                        ) { income ->
                            IncomeCard(
                                income = income,
                                onClick = { onIncomeClick(income) },
                                onDeleteClick = { viewModel.onRequestDelete(income) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                is IncomeListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeCard(
    income: Income,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.personalexpensetracker.ui.components.FinlyTransactionRow(
        title = income.title,
        category = income.source,
        dateText = com.personalexpensetracker.ui.util.DateFormatter.format(income.date, com.personalexpensetracker.ui.util.currentDateFormat),
        amountText = formatIncomeAmount(income.amount),
        isExpense = false,
        onClick = onClick,
        onDeleteClick = onDeleteClick,
        modifier = modifier
    )
}

@Composable
private fun formatIncomeAmount(amount: BigDecimal): String {
    return CurrencyFormatter.format(amount, currentCurrency)
}

