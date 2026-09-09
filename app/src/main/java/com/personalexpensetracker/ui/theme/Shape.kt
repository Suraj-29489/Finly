package com.personalexpensetracker.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================================
// FINLY SHAPES & CORNER RADII
// ============================================================================

val FinlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Specific Reusable Shapes
val FinlyCardShape = RoundedCornerShape(20.dp)
val FinlyCardShapeSmall = RoundedCornerShape(14.dp)
val FinlyButtonShape = RoundedCornerShape(14.dp)
val FinlyIconSquircleShape = RoundedCornerShape(12.dp)
val FinlyPillShape = RoundedCornerShape(999.dp)
val FinlyDialogShape = RoundedCornerShape(24.dp)
val FinlyBottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val FinlyCircleShape = CircleShape
