package com.personalexpensetracker.ui.screens.analytics

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.domain.model.FinancialPeriod
import com.personalexpensetracker.domain.model.MonthlyCashFlowTrend
import com.personalexpensetracker.ui.components.FinlyCard
import com.personalexpensetracker.ui.components.FinlyEmptyState
import com.personalexpensetracker.ui.components.FinlyIconButton
import com.personalexpensetracker.ui.components.FinlySectionHeader
import com.personalexpensetracker.ui.theme.FinlyCardShapeSmall
import com.personalexpensetracker.ui.theme.FinlyIconSquircleShape
import com.personalexpensetracker.ui.theme.FinlyPillShape
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer
import com.personalexpensetracker.ui.theme.StatusError
import com.personalexpensetracker.ui.theme.StatusErrorContainer
import com.personalexpensetracker.ui.theme.StatusSuccess
import com.personalexpensetracker.ui.theme.StatusSuccessContainer
import com.personalexpensetracker.ui.util.CurrencyFormatter
import com.personalexpensetracker.ui.util.currentCurrency
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CashFlowAnalyticsScreen(
    viewModel: CashFlowAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        text = "ANALYTICS & REPORTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Cash Flow & Trends",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item {
                    // Period selection pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinancialPeriod.values().forEach { period ->
                            val isSelected = uiState.selectedPeriod == period
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onPeriodSelected(period) },
                                label = {
                                    Text(
                                        text = period.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

                // 1. HERO PERIOD SUMMARY CARD
                item {
                    PeriodSummaryHeroCard(
                        period = uiState.selectedPeriod,
                        income = uiState.summary.totalIncome,
                        expenses = uiState.summary.totalExpenses,
                        netCashFlow = uiState.summary.netCashFlow,
                        incomeCount = uiState.incomeCount,
                        expenseCount = uiState.expenseCount
                    )
                }

                // 2. HISTORICAL MONTHLY TRENDS SECTION
                item {
                    FinlySectionHeader(
                        title = "Historical Monthly Trends"
                    )
                }

                if (uiState.monthlyTrends.isEmpty() || uiState.monthlyTrends.all { it.income == BigDecimal.ZERO && it.expenses == BigDecimal.ZERO }) {
                    item {
                        FinlyEmptyState(
                            icon = Icons.Outlined.BarChart,
                            title = "No trend data yet",
                            subtitle = "Recorded transactions will automatically appear here as monthly cash flow comparisons.",
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(uiState.monthlyTrends.reversed()) { trend ->
                        MonthlyTrendCard(trend = trend)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun PeriodSummaryHeroCard(
    period: FinancialPeriod,
    income: BigDecimal,
    expenses: BigDecimal,
    netCashFlow: BigDecimal,
    incomeCount: Int,
    expenseCount: Int,
    modifier: Modifier = Modifier
) {
    val currency = currentCurrency
    val isPositive = netCashFlow >= BigDecimal.ZERO
    val statusColor = if (isPositive) StatusSuccess else StatusError

    // Calculate ratio for visual cash flow progress bar
    val totalVolume = income.add(expenses)
    val incomeRatio = if (totalVolume > BigDecimal.ZERO) {
        (income.toDouble() / totalVolume.toDouble()).toFloat()
    } else {
        0.5f
    }

    FinlyCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Period Badge & Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${period.label.uppercase()} NET CASH FLOW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )

                Surface(
                    color = if (isPositive) StatusSuccessContainer else StatusErrorContainer,
                    shape = FinlyPillShape
                ) {
                    Text(
                        text = if (isPositive) "SURPLUS" else "DEFICIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Net Cash Flow Hero Amount
            Text(
                text = (if (isPositive) "+ " else "- ") + CurrencyFormatter.format(netCashFlow.abs(), currency),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            // Visual Ratio Bar
            if (totalVolume > BigDecimal.ZERO) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inflow ${(incomeRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Outflow ${((1f - incomeRatio) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusError,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { incomeRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusSuccess,
                        trackColor = StatusError,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Inflow & Outflow Detail Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Inflow
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(FinlyIconSquircleShape)
                                .background(StatusSuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "Inflow",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "+ " + CurrencyFormatter.format(income, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "$incomeCount txns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Outflow
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Outflow",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(FinlyIconSquircleShape)
                                .background(StatusErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = "- " + CurrencyFormatter.format(expenses, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$expenseCount txns",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendCard(
    trend: MonthlyCashFlowTrend,
    modifier: Modifier = Modifier
) {
    val currency = currentCurrency
    val monthName = trend.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    val isPositive = trend.netCashFlow >= BigDecimal.ZERO
    val statusColor = if (isPositive) StatusSuccess else StatusError

    FinlyCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "In: +${CurrencyFormatter.format(trend.income, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = StatusSuccess
                    )
                    Text(
                        text = "Out: -${CurrencyFormatter.format(trend.expenses, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = if (isPositive) StatusSuccessContainer else StatusErrorContainer,
                shape = FinlyPillShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = CurrencyFormatter.format(trend.netCashFlow.abs(), currency),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}
