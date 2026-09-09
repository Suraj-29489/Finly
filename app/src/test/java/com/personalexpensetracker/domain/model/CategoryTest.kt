package com.personalexpensetracker.domain.model

import com.personalexpensetracker.ui.theme.CategoryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused unit tests verifying the centralized category foundation in Finly.
 */
class CategoryTest {

    @Test
    fun builtInCategories_containsAllNineRequiredCategoriesInExactOrder() {
        val expected = listOf(
            "Food",
            "Transportation",
            "Shopping",
            "Bills",
            "Entertainment",
            "Health",
            "Education",
            "Travel",
            "Other"
        )
        assertEquals(expected, Category.BUILT_IN_CATEGORIES)
        assertEquals(9, Category.BUILT_IN_CATEGORIES.size)
    }

    @Test
    fun defaultCategory_isFood() {
        assertEquals("Food", Category.DEFAULT)
    }

    @Test
    fun filterCategories_startsWithAll_followedByAllBuiltInCategories() {
        val expected = listOf("All") + Category.BUILT_IN_CATEGORIES
        assertEquals(expected, Category.FILTER_CATEGORIES)
        assertEquals("All", Category.FILTER_CATEGORIES.first())
        assertEquals(10, Category.FILTER_CATEGORIES.size)
    }

    @Test
    fun isBuiltIn_validatesCategoriesCaseInsensitively() {
        assertTrue(Category.isBuiltIn("Food"))
        assertTrue(Category.isBuiltIn("food"))
        assertTrue(Category.isBuiltIn("FOOD"))
        assertTrue(Category.isBuiltIn("  Transportation  "))
        assertTrue(Category.isBuiltIn("Shopping"))
        assertTrue(Category.isBuiltIn("bills"))
        assertTrue(Category.isBuiltIn("ENTERTAINMENT"))
        assertTrue(Category.isBuiltIn("health"))
        assertTrue(Category.isBuiltIn("Education"))
        assertTrue(Category.isBuiltIn("travel"))
        assertTrue(Category.isBuiltIn("Other"))

        assertFalse(Category.isBuiltIn("Unknown"))
        assertFalse(Category.isBuiltIn("CustomCategory"))
        assertFalse(Category.isBuiltIn(""))
        assertFalse(Category.isBuiltIn("   "))
    }

    @Test
    fun normalize_returnsCanonicalBuiltInName() {
        assertEquals("Food", Category.normalize("food"))
        assertEquals("Food", Category.normalize("FOOD"))
        assertEquals("Food", Category.normalize("  Food  "))
        assertEquals("Transportation", Category.normalize("transportation"))
        assertEquals("Shopping", Category.normalize("SHOPPING"))
        assertEquals("Bills", Category.normalize("bills"))
        assertEquals("Entertainment", Category.normalize("entertainment"))
        assertEquals("Health", Category.normalize("HEALTH"))
        assertEquals("Education", Category.normalize("education"))
        assertEquals("Travel", Category.normalize("TRAVEL"))
        assertEquals("Other", Category.normalize("other"))

        // Unrecognized or custom preserves trimmed input
        assertEquals("Custom Category", Category.normalize("  Custom Category  "))
    }

    @Test
    fun noDuplicatesInBuiltInCategories() {
        val distinctCount = Category.BUILT_IN_CATEGORIES.map { it.lowercase() }.distinct().size
        assertEquals(Category.BUILT_IN_CATEGORIES.size, distinctCount)
    }

    @Test
    fun allBuiltInCategoriesHaveValidCategoryThemeColors() {
        Category.BUILT_IN_CATEGORIES.forEach { category ->
            val color = CategoryTheme.getColor(category)
            assertNotEquals(
                "Color for $category should not be unspecified",
                androidx.compose.ui.graphics.Color.Unspecified,
                color
            )
        }
        val allColor = CategoryTheme.getColor(Category.ALL)
        assertNotEquals(androidx.compose.ui.graphics.Color.Unspecified, allColor)
    }
}

