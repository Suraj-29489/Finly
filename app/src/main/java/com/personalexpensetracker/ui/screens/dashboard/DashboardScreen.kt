package com.personalexpensetracker.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.ui.components.FinlyCard
import com.personalexpensetracker.ui.components.FinlyEmptyState
import com.personalexpensetracker.ui.components.FinlySectionHeader
import com.personalexpensetracker.ui.components.FinlyStatCard
import com.personalexpensetracker.ui.components.FinlyTransactionRow
import com.personalexpensetracker.ui.theme.FinlyCardShape
import com.personalexpensetracker.ui.theme.FinlyGreen
import com.personalexpensetracker.ui.theme.FinlyGreenContainer
import com.personalexpensetracker.ui.theme.FinlyIconSquircleShape
import com.personalexpensetracker.ui.theme.FinlyPillShape
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer
import com.personalexpensetracker.ui.theme.FinlyRed
import com.personalexpensetracker.ui.theme.FinlyRedContainer
import com.personalexpensetracker.ui.util.CurrencyFormatter
import com.personalexpensetracker.ui.util.DateFormatter
import com.personalexpensetracker.ui.util.currentCurrency
import com.personalexpensetracker.ui.util.currentDateFormat
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Redesigned Finly Dashboard Screen:
 * - Top greeting header with current period pill
 * - Prominent Total Net Balance hero card with inflow & outflow
 * - Interactive current month cash flow overview card
 * - Side-by-side quick financial metrics for today
 * - Recent transactions section with category squircles and "Auto" badges
 * - Clean empty and loading states
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onExpenseClick: (Expense) -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {},
    onCashFlowClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = FinlyPurple,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Loading your finances...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is DashboardUiState.Empty -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DashboardHeader()
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        FinlyEmptyState(
                            title = "Welcome to Finly",
                            subtitle = "Start tracking your finances today. Add your first expense or income to see your balance and monthly insights.",
                            icon = Icons.Outlined.Savings,
                            actionText = "Add First Expense",
                            onActionClick = onNavigateToAddExpense
                        )
                    }
                }
            }

            is DashboardUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    // 1. Greeting & Period Header + Hero "Spends This Month" Card
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardHeader()
                            SpendsThisMonthHeroCard(
                                monthlyTotal = state.currentMonthTotal,
                                transactionCount = state.currentMonthTransactionCount,
                                todayTotal = state.todayTotal
                            )
                        }
                    }

                    // 2. CRITICAL CHANGE #2: Expense-focused Spending Summary Row (NO INCOME)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FinlyStatCard(
                                title = "Today's Spending",
                                value = formatCurrency(state.todayTotal),
                                icon = Icons.Outlined.ShoppingCart,
                                iconTint = FinlyPurple,
                                iconBackground = FinlyPurpleContainer,
                                modifier = Modifier.weight(1f)
                            )

                            FinlyStatCard(
                                title = "This Month",
                                value = "${state.currentMonthTransactionCount} txns",
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                                iconTint = FinlyPurple,
                                iconBackground = FinlyPurpleContainer,
                                subtitle = "Recorded expenses",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. TOP CATEGORIES SECTION
                    if (state.topCategories.isNotEmpty()) {
                        item {
                            FinlySectionHeader(
                                title = "Top Categories",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        item {
                            FinlyCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.topCategories.forEachIndexed { index, catSpend ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                com.personalexpensetracker.ui.components.FinlyCategoryIcon(
                                                    category = catSpend.category,
                                                    size = 36.dp,
                                                    iconSize = 18.dp
                                                )
                                                Column {
                                                    Text(
                                                        text = catSpend.category,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (catSpend.percentage > 0) {
                                                        Text(
                                                            text = "${catSpend.percentage}% of month's spend",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = formatCurrency(catSpend.amount),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (index < state.topCategories.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Recent Transactions Section Header
                    item {
                        FinlySectionHeader(
                            title = "Recent Transactions",
                            actionText = if (state.recentTransactions.isNotEmpty()) "View All" else null,
                            onActionClick = onViewAllClick,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // 6. Recent Transactions List
                    if (state.recentTransactions.isEmpty()) {
                        item {
                            FinlyEmptyState(
                                title = "No recent transactions",
                                subtitle = "Your recent spending and auto-captured expenses will show up here.",
                                icon = Icons.AutoMirrored.Outlined.ReceiptLong
                            )
                        }
                    } else {
                        items(state.recentTransactions, key = { it.id }) { expense ->
                            val isAutoCaptured = expense.notes?.contains("Auto-captured", ignoreCase = true) == true
                            FinlyTransactionRow(
                                title = expense.title,
                                category = expense.category,
                                dateText = formatDate(expense.date),
                                amountText = formatCurrency(expense.amount),
                                isExpense = true,
                                isAutoCaptured = isAutoCaptured,
                                onClick = { onExpenseClick(expense) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top header displaying greeting and current month pill badge.
 */
@Composable
private fun DashboardHeader(
    modifier: Modifier = Modifier
) {
    val currentMonth = YearMonth.now()
    val monthName = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SPENDING AT A GLANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Finly Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .clip(FinlyPillShape)
                .background(FinlyPurpleContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = FinlyPurple
            )
        }
    }
}

/**
 * CRITICAL CHANGE #1: Spends This Month Hero Card
 * Visually dominant spending amount with modern fintech styling.
 */
@Composable
private fun SpendsThisMonthHeroCard(
    monthlyTotal: BigDecimal,
    transactionCount: Int,
    todayTotal: BigDecimal,
    modifier: Modifier = Modifier
) {
    FinlyCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPENDS THIS MONTH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Box(
                modifier = Modifier
                    .clip(FinlyPillShape)
                    .background(FinlyPurpleContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$transactionCount transactions",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinlyPurple,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large dominant financial amount
        Text(
            text = formatCurrency(monthlyTotal),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(FinlyPillShape)
                        .background(FinlyPurple)
                )
                Text(
                    text = "Today's spend: " + formatCurrency(todayTotal),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Automatic & Manual",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Interactive card highlighting current calendar month cash flow and spending metrics.
 */
@Composable
private fun CurrentMonthCard(
    monthlyExpenses: BigDecimal,
    monthlyIncome: BigDecimal,
    monthlyCashFlow: BigDecimal,
    transactionCount: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPositive = monthlyCashFlow >= BigDecimal.ZERO
    val cashFlowColor = if (isPositive) FinlyGreen else FinlyRed
    val cashFlowText = if (isPositive) {
        "+ " + formatCurrency(monthlyCashFlow)
    } else {
        "- " + formatCurrency(monthlyCashFlow.abs())
    }

    FinlyCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "THIS MONTH CASH FLOW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "View Analytics",
                    tint = FinlyPurple,
                    modifier = Modifier.size(14.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(FinlyPillShape)
                    .background(FinlyPurpleContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$transactionCount txns",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinlyPurple,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = cashFlowText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = cashFlowColor
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Income:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+ " + formatCurrency(monthlyIncome),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = FinlyGreen
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Expenses:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(monthlyExpenses),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun formatDate(instant: Instant): String {
    return DateFormatter.format(instant, currentDateFormat)
}

@Composable
private fun formatCurrency(amount: BigDecimal): String {
    return CurrencyFormatter.format(amount, currentCurrency)
}
