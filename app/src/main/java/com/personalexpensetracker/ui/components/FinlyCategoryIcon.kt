package com.personalexpensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personalexpensetracker.ui.theme.CategoryTheme
import com.personalexpensetracker.ui.theme.FinlyIconSquircleShape

/**
 * Standard category icon badge matching the reference visual style:
 * Soft pastel squircle container with a clean native Material icon inside.
 */
@Composable
fun FinlyCategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    customIcon: ImageVector? = null,
    customTint: Color? = null,
    customBackground: Color? = null
) {
    val meta = CategoryTheme.getMeta(category)
    val backgroundColor = customBackground ?: meta.containerColor
    val tintColor = customTint ?: meta.color
    val icon = customIcon ?: meta.icon

    Box(
        modifier = modifier
            .size(size)
            .clip(FinlyIconSquircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
