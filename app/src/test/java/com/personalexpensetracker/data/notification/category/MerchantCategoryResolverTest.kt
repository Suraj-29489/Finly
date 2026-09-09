package com.personalexpensetracker.data.notification.category

import com.personalexpensetracker.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantCategoryResolverTest {

    @Test
    fun `resolveCategory - maps known food merchants to FOOD`() {
        assertEquals(Category.FOOD, MerchantCategoryResolver.resolveCategory("Swiggy"))
        assertEquals(Category.FOOD, MerchantCategoryResolver.resolveCategory("Zomato"))
        assertEquals(Category.FOOD, MerchantCategoryResolver.resolveCategory("Dominos Pizza"))
        assertEquals(Category.FOOD, MerchantCategoryResolver.resolveCategory("Blinkit"))
        assertEquals(Category.FOOD, MerchantCategoryResolver.resolveCategory("Zepto"))
        assertEquals(Category.FOOD, MerchantCategoryResolver.resolveCategory("McDonald's"))
    }

    @Test
    fun `resolveCategory - maps known transportation merchants to TRANSPORTATION`() {
        assertEquals(Category.TRANSPORTATION, MerchantCategoryResolver.resolveCategory("Uber"))
        assertEquals(Category.TRANSPORTATION, MerchantCategoryResolver.resolveCategory("Ola Cabs"))
        assertEquals(Category.TRANSPORTATION, MerchantCategoryResolver.resolveCategory("Rapido"))
        assertEquals(Category.TRANSPORTATION, MerchantCategoryResolver.resolveCategory("IRCTC"))
        assertEquals(Category.TRANSPORTATION, MerchantCategoryResolver.resolveCategory("HP Petrol Pump"))
    }

    @Test
    fun `resolveCategory - maps known shopping merchants to SHOPPING`() {
        assertEquals(Category.SHOPPING, MerchantCategoryResolver.resolveCategory("Amazon"))
        assertEquals(Category.SHOPPING, MerchantCategoryResolver.resolveCategory("Flipkart"))
        assertEquals(Category.SHOPPING, MerchantCategoryResolver.resolveCategory("Myntra"))
        assertEquals(Category.SHOPPING, MerchantCategoryResolver.resolveCategory("Nykaa"))
        assertEquals(Category.SHOPPING, MerchantCategoryResolver.resolveCategory("Croma"))
    }

    @Test
    fun `resolveCategory - maps known bills merchants to BILLS`() {
        assertEquals(Category.BILLS, MerchantCategoryResolver.resolveCategory("Airtel"))
        assertEquals(Category.BILLS, MerchantCategoryResolver.resolveCategory("Jio Prepaid"))
        assertEquals(Category.BILLS, MerchantCategoryResolver.resolveCategory("Tata Power Electricity"))
        assertEquals(Category.BILLS, MerchantCategoryResolver.resolveCategory("BESCOM"))
        assertEquals(Category.BILLS, MerchantCategoryResolver.resolveCategory("LIC Insurance"))
    }

    @Test
    fun `resolveCategory - maps known entertainment merchants to ENTERTAINMENT`() {
        assertEquals(Category.ENTERTAINMENT, MerchantCategoryResolver.resolveCategory("Netflix"))
        assertEquals(Category.ENTERTAINMENT, MerchantCategoryResolver.resolveCategory("Spotify"))
        assertEquals(Category.ENTERTAINMENT, MerchantCategoryResolver.resolveCategory("BookMyShow"))
        assertEquals(Category.ENTERTAINMENT, MerchantCategoryResolver.resolveCategory("PVR Cinemas"))
    }

    @Test
    fun `resolveCategory - maps known health merchants to HEALTH`() {
        assertEquals(Category.HEALTH, MerchantCategoryResolver.resolveCategory("Apollo Pharmacy"))
        assertEquals(Category.HEALTH, MerchantCategoryResolver.resolveCategory("PharmEasy"))
        assertEquals(Category.HEALTH, MerchantCategoryResolver.resolveCategory("1mg"))
        assertEquals(Category.HEALTH, MerchantCategoryResolver.resolveCategory("Cult.fit"))
    }

    @Test
    fun `resolveCategory - falls back to OTHER for unknown merchants`() {
        assertEquals(Category.OTHER, MerchantCategoryResolver.resolveCategory("Unknown Vendor 123"))
        assertEquals(Category.OTHER, MerchantCategoryResolver.resolveCategory("ABCXYZ9999"))
        assertEquals(Category.OTHER, MerchantCategoryResolver.resolveCategory("Unknown Merchant"))
        assertEquals(Category.OTHER, MerchantCategoryResolver.resolveCategory(null))
        assertEquals(Category.OTHER, MerchantCategoryResolver.resolveCategory(""))
    }
}

