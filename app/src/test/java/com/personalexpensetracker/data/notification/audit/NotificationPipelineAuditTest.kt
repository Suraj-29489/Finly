package com.personalexpensetracker.data.notification.audit

import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.notification.parser.TransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Verification Test:
 * Programmatically confirms that all 7 identified root causes
 * in the notification pipeline are completely resolved.
 */
class NotificationPipelineAuditTest {

    // Cause 1: Title preserved and used for amount and merchant fallback
    @Test
    fun `verify Cause 1 Fixed - Title is preserved and used for amount and merchant fallback`() {
        // Case A: Amount in title, body has payee only
        val notifAmountInTitle = RawNotificationData(
            packageName = "com.hdfc.banking",
            title = "HDFC Bank: ₹1,500 debited",
            text = "to Amazon India",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "k1"
        )
        val parsedA = TransactionParser.parse(notifAmountInTitle, TransactionDirection.DEBIT)
        assertNotNull(parsedA)
        assertEquals(BigDecimal("1500.00"), parsedA?.amount)
        assertEquals("Amazon India", parsedA?.merchant)

        // Case B: Payee in title, body has amount only
        val notifMerchantInTitle = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Swiggy",
            text = "Paid ₹450 using UPI",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "k2"
        )
        val parsedB = TransactionParser.parse(notifMerchantInTitle, TransactionDirection.DEBIT)
        assertNotNull(parsedB)
        assertEquals(BigDecimal("450.00"), parsedB?.amount)
        assertEquals("Swiggy", parsedB?.merchant)
    }

    // Cause 2: "from Account XXXX" rejected as merchant, actual payee captured
    @Test
    fun `verify Cause 2 Fixed - from Ac rejects bank account and extracts actual payee`() {
        val text = "INR 500 debited from Account 1234 on 08-09-26 for Swiggy"
        val extractedMerchant = TransactionParser.extractMerchant(text)
        assertEquals("Swiggy", extractedMerchant)
        assertNotEquals("Account 1234", extractedMerchant)
    }

    // Cause 3: UPI VPAs with @ and / are properly extracted
    @Test
    fun `verify Cause 3 Fixed - UPI VPAs with at and slash are properly supported`() {
        val text = "Paid ₹250 to swiggy@icici"
        val extractedMerchant = TransactionParser.extractMerchant(text)
        assertEquals("swiggy@icici", extractedMerchant)
    }

    // Cause 4: Direction classifier correctly handles "Sent ₹500 to Ramesh"
    @Test
    fun `verify Cause 4 Fixed - Sent or Transferred with intervening amount classified as DEBIT`() {
        val text1 = "Sent ₹500 to Ramesh via GPay"
        val text2 = "Transferred INR 1,000 to Swiggy"

        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text1))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text2))
    }

    // Cause 5: Amount without currency prefix successfully extracted after debit verbs
    @Test
    fun `verify Cause 5 Fixed - Amount without currency prefix successfully extracted`() {
        val text = "A/c XXXX1234 debited by 450.00 on 08Sep26"
        val amount = TransactionParser.extractAmount(text)
        assertEquals(BigDecimal("450.00"), amount)
    }

    // Cause 6: Available balance ignored in favor of actual debit amount
    @Test
    fun `verify Cause 6 Fixed - Available balance preceding debit amount is ignored`() {
        val text = "Avl Bal: INR 25,000.00. Your A/c has been debited for INR 450.00"
        val amount = TransactionParser.extractAmount(text)
        assertEquals(BigDecimal("450.00"), amount)
        assertNotEquals(BigDecimal("25000.00"), amount)
    }

    // Cause 7: RawNotificationData supports EXTRA_TEXT_LINES and EXTRA_MESSAGES
    @Test
    fun `verify Cause 7 Fixed - Notification listener handles textLines and messages`() {
        val raw = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = null,
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "msg_1",
            messages = listOf("INR 350.00 debited for Uber")
        )
        assertEquals(true, raw.hasContent)
        assertEquals("INR 350.00 debited for Uber", raw.fullText)
        assertEquals("VK-HDFCBK INR 350.00 debited for Uber", raw.combinedText)

        val parsed = TransactionParser.parse(raw, TransactionDirection.DEBIT)
        assertEquals(BigDecimal("350.00"), parsed?.amount)
        assertEquals("Uber", parsed?.merchant)
    }
}
