package com.personalexpensetracker.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.ui.components.FinlyEmptyState
import com.personalexpensetracker.ui.components.FinlyTransactionRow
import com.personalexpensetracker.ui.theme.FinlyCardShape
import com.personalexpensetracker.ui.theme.FinlyPillShape
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer
import com.personalexpensetracker.ui.util.CurrencyFormatter
import com.personalexpensetracker.ui.util.DateFormatter
import com.personalexpensetracker.ui.util.currentCurrency
import com.personalexpensetracker.ui.util.currentDateFormat
import java.math.BigDecimal
import java.time.Instant

/**
 * Modern Finly Expense List Screen:
 * - Search bar with clean purple accent
 * - Filter chips for categories and date filters
 * - Sort options dropdown
 * - Compact transaction rows using FinlyTransactionRow
 * - Native auto-capture badges
 * - Clean empty and error states
 */
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel,
    onExpenseClick: (Expense) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingDelete by viewModel.pendingDeleteExpense.collectAsState()
    val deletionError by viewModel.deletionError.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val hasActiveFilters by viewModel.hasActiveFilters.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Confirmation dialog for deletion
    if (pendingDelete != null) {
        val expense = pendingDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = FinlyCardShape,
            title = {
                Text(
                    text = "Delete Expense?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${expense.title}\" (${formatAmount(expense.amount)})? This action cannot be undone.",
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
            if (deletionError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
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
                        TextButton(onClick = { viewModel.clearDeletionError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Search & Filter controls
            if (uiState !is ExpenseListUiState.Empty && uiState !is ExpenseListUiState.Error && uiState !is ExpenseListUiState.Loading) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = {
                        Text(
                            text = "Search description or category...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotEmpty()) FinlyPurple else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = FinlyPurple
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinlyPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = FinlyPurple
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableCategories) { category ->
                        val isSelected = (category == selectedCategory)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategorySelect(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
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

                // Date Filter and Sort Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpenseDateFilter.entries.forEach { dateFilter ->
                        val isSelected = (dateFilter == selectedDateFilter)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onDateFilterSelect(dateFilter) },
                            label = {
                                Text(
                                    text = dateFilter.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
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

                    Box {
                        val isSortActive = (selectedSortOrder != ExpenseSortOrder.NEWEST_FIRST)
                        FilterChip(
                            selected = isSortActive,
                            onClick = { sortMenuExpanded = true },
                            label = {
                                Text(
                                    text = "Sort: ${selectedSortOrder.displayName}",
                                    fontWeight = if (isSortActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = FinlyPurpleContainer,
                                selectedLabelColor = FinlyPurple
                            ),
                            border = if (isSortActive) {
                                BorderStroke(1.dp, FinlyPurple.copy(alpha = 0.5f))
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                            shape = FinlyPillShape
                        )

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            ExpenseSortOrder.entries.forEach { sortOrder ->
                                val isCurrent = (sortOrder == selectedSortOrder)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOrder.displayName,
                                            color = if (isCurrent) FinlyPurple else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.onSortOrderSelect(sortOrder)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (hasActiveFilters) {
                        AssistChip(
                            onClick = { viewModel.resetFilters() },
                            label = {
                                Text(
                                    text = "Reset",
                                    color = FinlyPurple,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset filters",
                                    tint = FinlyPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = FinlyPurpleContainer,
                                labelColor = FinlyPurple
                            ),
                            border = null,
                            shape = FinlyPillShape
                        )
                    }
                }
            }

            when (val state = uiState) {
                is ExpenseListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = FinlyPurple,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Loading expenses...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is ExpenseListUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FinlyEmptyState(
                            title = "No expenses yet",
                            subtitle = "Transactions you add or auto-capture will appear here.",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong
                        )
                    }
                }

                is ExpenseListUiState.NoMatchingResults -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FinlyEmptyState(
                            title = "No matching expenses",
                            subtitle = "No expenses match your search or filter criteria.",
                            icon = Icons.Outlined.SearchOff,
                            actionText = "Reset Filters",
                            onActionClick = { viewModel.resetFilters() }
                        )
                    }
                }

                is ExpenseListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = FinlyCardShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Unable to load expenses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                is ExpenseListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.expenses, key = { it.id }) { expense ->
                            val isAutoCaptured = expense.notes?.contains("Auto-captured", ignoreCase = true) == true
                            FinlyTransactionRow(
                                title = expense.title,
                                category = expense.category,
                                dateText = formatDate(expense.date),
                                amountText = formatAmount(expense.amount),
                                isExpense = true,
                                isAutoCaptured = isAutoCaptured,
                                onClick = { onExpenseClick(expense) },
                                onDeleteClick = { viewModel.requestDelete(expense) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatAmount(amount: BigDecimal): String {
    return CurrencyFormatter.format(amount, currentCurrency)
}

@Composable
private fun formatDate(instant: Instant): String {
    return DateFormatter.format(instant, currentDateFormat)
}

