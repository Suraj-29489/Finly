package com.personalexpensetracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.FinlyPurpleContainer

enum class FinlyNavigationDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    REPORTS("Report", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PLAN("Plan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * Modern Finly Bottom Navigation bar matching the reference fintech design:
 * - Clean white container with subtle top border
 * - 4 destinations: Home, Report, Plan, Settings
 * - Prominent centered floating purple '+' Add button
 */
@Composable
fun FinlyBottomNavigation(
    selectedDestination: FinlyNavigationDestination,
    onDestinationSelected: (FinlyNavigationDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Navigation Bar Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 items (Home & Reports)
                FinlyNavItem(
                    destination = FinlyNavigationDestination.HOME,
                    isSelected = selectedDestination == FinlyNavigationDestination.HOME,
                    onClick = { onDestinationSelected(FinlyNavigationDestination.HOME) },
                    modifier = Modifier.weight(1f)
                )

                FinlyNavItem(
                    destination = FinlyNavigationDestination.REPORTS,
                    isSelected = selectedDestination == FinlyNavigationDestination.REPORTS,
                    onClick = { onDestinationSelected(FinlyNavigationDestination.REPORTS) },
                    modifier = Modifier.weight(1f)
                )

                // Spacer for centered Add button
                Spacer(modifier = Modifier.weight(1.1f))

                // Right 2 items (Plan & Settings)
                FinlyNavItem(
                    destination = FinlyNavigationDestination.PLAN,
                    isSelected = selectedDestination == FinlyNavigationDestination.PLAN,
                    onClick = { onDestinationSelected(FinlyNavigationDestination.PLAN) },
                    modifier = Modifier.weight(1f)
                )

                FinlyNavItem(
                    destination = FinlyNavigationDestination.SETTINGS,
                    isSelected = selectedDestination == FinlyNavigationDestination.SETTINGS,
                    onClick = { onDestinationSelected(FinlyNavigationDestination.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Central Prominent Floating '+' Action Button
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(56.dp)
                .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = FinlyPurple, spotColor = FinlyPurple)
                .clip(CircleShape)
                .background(FinlyPurple)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun FinlyNavItem(
    destination: FinlyNavigationDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (isSelected) destination.selectedIcon else destination.unselectedIcon
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) FinlyPurple else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "navIconTint"
    )
    val textTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "navTextTint"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = destination.title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = destination.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textTint,
            fontSize = 11.sp
        )
    }
}
