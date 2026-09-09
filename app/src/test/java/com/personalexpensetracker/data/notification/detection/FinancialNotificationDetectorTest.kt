package com.personalexpensetracker.data.notification.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialNotificationDetectorTest {

    @Test
    fun `isFinancialNotification - detects debit keywords with currency`() {
        assertTrue(FinancialNotificationDetector.isFinancialNotification("₹450 debited from A/c XXXX1234 for Swiggy"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Rs 1200 spent on Amazon using HDFC card"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("INR 250 paid to Zomato via UPI"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Rs. 500 withdrawn from ATM"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Payment of ₹350 to Uber successful"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Amount of ₹1,250.50 deducted from your account"))
    }

    @Test
    fun `isFinancialNotification - detects credit keywords with currency`() {
        assertTrue(FinancialNotificationDetector.isFinancialNotification("₹50,000 credited to your account"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Salary of Rs 75000 credited for August"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Refund of INR 450 received from Swiggy"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("Cashback of ₹50 received on UPI transaction"))
    }

    @Test
    fun `isFinancialNotification - detects financial terms without currency if digits present`() {
        assertTrue(FinancialNotificationDetector.isFinancialNotification("debited from account 1234"))
        assertTrue(FinancialNotificationDetector.isFinancialNotification("UPI transaction 98765 completed"))
    }

    @Test
    fun `isFinancialNotification - rejects OTP and verification codes without financial transaction`() {
        assertFalse(FinancialNotificationDetector.isFinancialNotification("Your OTP is 482913. Do not share with anyone."))
        assertFalse(FinancialNotificationDetector.isFinancialNotification("Your verification code is 123456"))
        assertFalse(FinancialNotificationDetector.isFinancialNotification("Use 987654 as your login PIN"))
    }

    @Test
    fun `isFinancialNotification - rejects promotional and general notifications`() {
        assertFalse(FinancialNotificationDetector.isFinancialNotification("Hey! Check out our 50% discount offer on shoes!"))
        assertFalse(FinancialNotificationDetector.isFinancialNotification("Update available for WhatsApp"))
        assertFalse(FinancialNotificationDetector.isFinancialNotification("Happy Birthday from your bank!"))
        assertFalse(FinancialNotificationDetector.isFinancialNotification("How was your ride with Uber? Rate us."))
    }

    @Test
    fun `isFinancialNotification - rejects empty and blank text`() {
        assertFalse(FinancialNotificationDetector.isFinancialNotification(""))
        assertFalse(FinancialNotificationDetector.isFinancialNotification("   "))
    }
}

