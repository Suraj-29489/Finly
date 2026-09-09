package com.personalexpensetracker.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================================================
// FINLY CORE COLOR SYSTEM
// ============================================================================

// Primary Accent (Modern Purple / Violet)
val FinlyPurple = Color(0xFF7C3AED)          // Modern primary violet
val FinlyPurpleLight = Color(0xFF8B5CF6)     // Vibrant light violet
val FinlyPurpleDark = Color(0xFF6D28D9)      // Deep violet
val FinlyPurpleContainer = Color(0xFFEDE9FE) // Soft pastel violet container
val FinlyOnPurpleContainer = Color(0xFF5B21B6) // Deep violet text on container
val FinlyPurpleSubtle = Color(0xFFF5F3FF)    // Very soft tint

// Light Surfaces & Foundations (Minimalist Fintech Aesthetic)
val FinlyBackgroundLight = Color(0xFFF8F9FA) // Clean off-white background
val FinlySurfaceLight = Color(0xFFFFFFFF)    // Pure white cards & elevated sheets
val FinlySurfaceSubtle = Color(0xFFF3F4F6)   // Input fields & subtle chips
val FinlyBorderLight = Color(0xFFEEF0F4)     // Crisp subtle card border
val FinlyBorderStrong = Color(0xFFE5E7EB)    // Input outline & active divider
val FinlyTextPrimary = Color(0xFF111827)     // Dark charcoal text
val FinlyTextSecondary = Color(0xFF6B7280)   // Balanced medium gray text
val FinlyTextMuted = Color(0xFF9CA3AF)       // Subtle supporting text

// Dark Foundations (Elevated Slate System)
val FinlyBackgroundDark = Color(0xFF0F172A)  // Deep slate dark background
val FinlySurfaceDark = Color(0xFF1E293B)     // Elevated slate surface card
val FinlySurfaceSubtleDark = Color(0xFF334155) // Dark input & chip fill
val FinlyBorderDark = Color(0xFF334155)      // Dark border
val FinlyTextPrimaryDark = Color(0xFFF8FAFC) // Crisp white text
val FinlyTextSecondaryDark = Color(0xFF94A3B8) // Muted slate secondary text
val FinlyTextMutedDark = Color(0xFF64748B)   // Dark muted text

// Functional / Financial Status Colors
val FinlyGreen = Color(0xFF10B981)           // Emerald green (Income / Inflow / Positive)
val FinlyGreenContainer = Color(0xFFECFDF5)  // Soft mint container
val FinlyOnGreenContainer = Color(0xFF065F46) // Deep emerald text
val FinlyGreenDark = Color(0xFF059669)

val FinlyRed = Color(0xFFEF4444)             // Crisp coral red (Expense / Error / Negative)
val FinlyRedContainer = Color(0xFFFEF2F2)    // Soft rose container
val FinlyOnRedContainer = Color(0xFF991B1B)  // Deep red text

val FinlyOrange = Color(0xFFF59E0B)          // Warm amber (Warnings / Pending)
val FinlyOrangeContainer = Color(0xFFFFFBEB)
val FinlyOnOrangeContainer = Color(0xFF92400E)

// ============================================================================
// CATEGORY ACCENTS & PASTEL CONTAINERS
// ============================================================================

val CategoryFood = Color(0xFFF59E0B)          // Warm Amber
val CategoryFoodContainer = Color(0xFFFEF3C7)

val CategoryTransport = Color(0xFF0EA5E9)     // Sky Blue
val CategoryTransportContainer = Color(0xFFE0F2FE)

val CategoryShopping = Color(0xFF8B5CF6)      // Soft Purple
val CategoryShoppingContainer = Color(0xFFEDE9FE)

val CategoryBills = Color(0xFF10B981)         // Emerald Green
val CategoryBillsContainer = Color(0xFFD1FAE5)
val CategoryUtilities = CategoryBills
val CategoryUtilitiesContainer = CategoryBillsContainer

val CategoryEntertainment = Color(0xFFEC4899) // Coral / Pink
val CategoryEntertainmentContainer = Color(0xFFFCE7F3)

val CategoryHealth = Color(0xFF14B8A6)        // Teal
val CategoryHealthContainer = Color(0xFFCCFBF1)

val CategoryEducation = Color(0xFF6366F1)     // Indigo
val CategoryEducationContainer = Color(0xFFE0E7FF)

val CategoryTravel = Color(0xFF0284C7)        // Deep Sky
val CategoryTravelContainer = Color(0xFFBAE6FD)

val CategoryOther = Color(0xFF64748B)         // Slate Neutral
val CategoryOtherContainer = Color(0xFFF1F5F9)

val CategoryAll = FinlyPurple
val CategoryAllContainer = FinlyPurpleContainer

data class CategoryMeta(
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector
)

object CategoryTheme {
    fun getColor(category: String): Color {
        return when (category.trim().lowercase()) {
            "food", "dining", "groceries", "grocery" -> CategoryFood
            "transportation", "transport", "commute", "fuel" -> CategoryTransport
            "shopping", "clothing", "electronics" -> CategoryShopping
            "bills", "utilities", "rent", "electricity", "water", "internet" -> CategoryBills
            "entertainment", "movies", "games", "leisure" -> CategoryEntertainment
            "health", "medical", "pharmacy", "fitness", "healthcare" -> CategoryHealth
            "education", "school", "books", "courses", "learning" -> CategoryEducation
            "travel", "trip", "vacation", "flight", "hotel" -> CategoryTravel
            "all" -> CategoryAll
            else -> CategoryOther
        }
    }

    fun getContainerColor(category: String): Color {
        return when (category.trim().lowercase()) {
            "food", "dining", "groceries", "grocery" -> CategoryFoodContainer
            "transportation", "transport", "commute", "fuel" -> CategoryTransportContainer
            "shopping", "clothing", "electronics" -> CategoryShoppingContainer
            "bills", "utilities", "rent", "electricity", "water", "internet" -> CategoryBillsContainer
            "entertainment", "movies", "games", "leisure" -> CategoryEntertainmentContainer
            "health", "medical", "pharmacy", "fitness", "healthcare" -> CategoryHealthContainer
            "education", "school", "books", "courses", "learning" -> CategoryEducationContainer
            "travel", "trip", "vacation", "flight", "hotel" -> CategoryTravelContainer
            "all" -> CategoryAllContainer
            else -> CategoryOtherContainer
        }
    }

    fun getIcon(category: String): ImageVector {
        return when (category.trim().lowercase()) {
            "food", "dining", "groceries", "grocery" -> Icons.Outlined.Restaurant
            "transportation", "transport", "commute", "fuel" -> Icons.Outlined.DirectionsCar
            "shopping", "clothing", "electronics" -> Icons.Outlined.ShoppingBag
            "bills", "utilities", "rent", "electricity", "water", "internet" -> Icons.AutoMirrored.Outlined.ReceiptLong
            "entertainment", "movies", "games", "leisure" -> Icons.Outlined.Movie
            "health", "medical", "pharmacy", "fitness", "healthcare" -> Icons.Outlined.LocalHospital
            "education", "school", "books", "courses", "learning" -> Icons.Outlined.School
            "travel", "trip", "vacation", "flight", "hotel" -> Icons.Outlined.Flight
            else -> Icons.Outlined.Category
        }
    }

    fun getMeta(category: String): CategoryMeta {
        val color = getColor(category)
        return CategoryMeta(
            color = color,
            containerColor = getContainerColor(category),
            icon = getIcon(category)
        )
    }
}

// ============================================================================
// BACKWARD COMPATIBILITY ALIASES
// ============================================================================
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val PrimaryBlue = FinlyPurple
val PrimaryBlueLight = FinlyPurpleLight
val PrimaryBlueDark = FinlyPurpleDark
val PrimaryBlueContainer = FinlyPurpleContainer
val OnPrimaryBlueContainer = FinlyOnPurpleContainer

val DarkBackground = FinlyBackgroundDark
val DarkSurface = FinlySurfaceDark
val DarkSurfaceVariant = FinlySurfaceSubtleDark
val DarkSurfaceHighlight = Color(0xFF28303D)
val DarkBorder = FinlyBorderDark
val DarkBorderSubtle = FinlyBorderDark

val TextPrimary = FinlyTextPrimary
val TextSecondary = FinlyTextSecondary
val TextMuted = FinlyTextMuted

val StatusSuccess = FinlyGreen
val StatusSuccessContainer = FinlyGreenContainer
val StatusOnSuccessContainer = FinlyOnGreenContainer

val StatusError = FinlyRed
val StatusErrorContainer = FinlyRedContainer
val StatusOnErrorContainer = FinlyOnRedContainer

val StatusWarning = FinlyOrange

