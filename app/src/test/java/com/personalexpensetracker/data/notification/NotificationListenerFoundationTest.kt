package com.personalexpensetracker.data.notification

import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Comprehensive test suite for the Notification Listener Foundation (Step 1).
 *
 * Tests cover:
 * 1. RawNotificationData model construction and properties
 * 2. BasicNotificationProcessor behavior
 * 3. Handling of empty/malformed notification data
 * 4. Content extraction helpers (fullText, combinedText, hasContent)
 * 5. NotificationProcessingResult sealed class variants
 */
class NotificationListenerFoundationTest {

    private lateinit var processor: BasicNotificationProcessor

    @Before
    fun setUp() {
        processor = BasicNotificationProcessor()
    }

    // ========================================================================
    // RawNotificationData Model Tests
    // ========================================================================

    @Test
    fun `RawNotificationData - complete notification has all fields`() {
        val data = createNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            text = "₹450 debited from A/c XXXX1234 for Swiggy",
            subText = "Transaction Alert",
            bigText = "₹450 debited from A/c XXXX1234 for Swiggy. Ref: UPI123456"
        )

        assertEquals("com.sbi.lotusintouch", data.packageName)
        assertEquals("SBI Alert", data.title)
        assertEquals("₹450 debited from A/c XXXX1234 for Swiggy", data.text)
        assertEquals("Transaction Alert", data.subText)
        assertEquals("₹450 debited from A/c XXXX1234 for Swiggy. Ref: UPI123456", data.bigText)
        assertNotNull(data.receivedAt)
        assertTrue(data.hasContent)
    }

    @Test
    fun `RawNotificationData - null title and text still constructs`() {
        val data = createNotificationData(
            title = null,
            text = null
        )

        assertNull(data.title)
        assertNull(data.text)
    }

    @Test
    fun `RawNotificationData - hasContent returns true when only title exists`() {
        val data = createNotificationData(title = "Alert", text = null)
        assertTrue(data.hasContent)
    }

    @Test
    fun `RawNotificationData - hasContent returns true when only text exists`() {
        val data = createNotificationData(title = null, text = "Some text")
        assertTrue(data.hasContent)
    }

    @Test
    fun `RawNotificationData - hasContent returns true when only subText exists`() {
        val data = createNotificationData(title = null, text = null, subText = "Sub text")
        assertTrue(data.hasContent)
    }

    @Test
    fun `RawNotificationData - hasContent returns true when only bigText exists`() {
        val data = createNotificationData(title = null, text = null, bigText = "Big text content")
        assertTrue(data.hasContent)
    }

    @Test
    fun `RawNotificationData - hasContent returns false when all text fields are null`() {
        val data = createNotificationData(title = null, text = null, subText = null, bigText = null)
        assertFalse(data.hasContent)
    }

    @Test
    fun `RawNotificationData - hasContent returns false when all text fields are blank`() {
        val data = createNotificationData(title = "", text = "  ", subText = "", bigText = "   ")
        assertFalse(data.hasContent)
    }

    @Test
    fun `RawNotificationData - fullText prefers bigText over text`() {
        val data = createNotificationData(
            text = "Short text",
            bigText = "Full expanded big text content"
        )
        assertEquals("Full expanded big text content", data.fullText)
    }

    @Test
    fun `RawNotificationData - fullText falls back to text when bigText is null`() {
        val data = createNotificationData(
            text = "Short text",
            bigText = null
        )
        assertEquals("Short text", data.fullText)
    }

    @Test
    fun `RawNotificationData - fullText falls back to text when bigText is blank`() {
        val data = createNotificationData(
            text = "Short text",
            bigText = "   "
        )
        assertEquals("Short text", data.fullText)
    }

    @Test
    fun `RawNotificationData - fullText returns null when both text and bigText are null`() {
        val data = createNotificationData(text = null, bigText = null)
        assertNull(data.fullText)
    }

    @Test
    fun `RawNotificationData - combinedText joins all available text`() {
        val data = createNotificationData(
            title = "SBI Alert",
            text = "₹450 debited",
            subText = "Transaction",
            bigText = "Full details"
        )
        assertEquals("SBI Alert ₹450 debited Transaction Full details", data.combinedText)
    }

    @Test
    fun `RawNotificationData - combinedText skips null and blank fields`() {
        val data = createNotificationData(
            title = "Alert",
            text = null,
            subText = "",
            bigText = "Details"
        )
        assertEquals("Alert Details", data.combinedText)
    }

    @Test
    fun `RawNotificationData - combinedText returns empty string when all null`() {
        val data = createNotificationData(title = null, text = null, subText = null, bigText = null)
        assertEquals("", data.combinedText)
    }

    // ========================================================================
    // BasicNotificationProcessor Tests
    // ========================================================================

    @Test
    fun `processor - valid notification with content returns Received`() = runTest {
        val data = createNotificationData(
            title = "SBI Alert",
            text = "₹450 debited from A/c XXXX1234 for Swiggy"
        )

        val result = processor.process(data)

        assertTrue(result is NotificationProcessingResult.Received)
        val received = result as NotificationProcessingResult.Received
        assertEquals(data, received.data)
    }

    @Test
    fun `processor - notification with only title returns Received`() = runTest {
        val data = createNotificationData(title = "Bank Alert", text = null)

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `processor - notification with only text returns Received`() = runTest {
        val data = createNotificationData(title = null, text = "₹100 paid to Amazon")

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `processor - notification with all null text fields returns Ignored`() = runTest {
        val data = createNotificationData(title = null, text = null, subText = null, bigText = null)

        val result = processor.process(data)

        assertTrue(result is NotificationProcessingResult.Ignored)
        val ignored = result as NotificationProcessingResult.Ignored
        assertEquals("Notification has no text content", ignored.reason)
    }

    @Test
    fun `processor - notification with all blank text fields returns Ignored`() = runTest {
        val data = createNotificationData(title = "", text = "  ", subText = "", bigText = "  ")

        val result = processor.process(data)

        assertTrue(result is NotificationProcessingResult.Ignored)
    }

    @Test
    fun `processor - bank notification returns Received`() = runTest {
        val data = createNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI",
            text = "₹1500 debited from your account"
        )

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `processor - UPI notification returns Received`() = runTest {
        val data = createNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "GPay",
            text = "Payment of ₹250 to Zomato successful"
        )

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `processor - card notification returns Received`() = runTest {
        val data = createNotificationData(
            packageName = "com.hdfc.mobileapp",
            title = "HDFC Bank",
            text = "Your HDFC credit card ending 1234 has been charged ₹3500 at Amazon"
        )

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `processor - promotional notification returns Received at step 1`() = runTest {
        // At Step 1, we don't filter financial vs non-financial — that's Step 2
        val data = createNotificationData(
            packageName = "com.whatsapp",
            title = "New message",
            text = "Hey, how are you?"
        )

        val result = processor.process(data)
        assertTrue("Step 1 should accept all notifications with content", result is NotificationProcessingResult.Received)
    }

    @Test
    fun `processor - credit notification returns Received at step 1`() = runTest {
        // At Step 1, we don't classify debit vs credit — that's Step 3
        val data = createNotificationData(
            title = "SBI Alert",
            text = "₹50000 credited to your account"
        )

        val result = processor.process(data)
        assertTrue("Step 1 should accept credits too — classification is Step 3", result is NotificationProcessingResult.Received)
    }

    // ========================================================================
    // NotificationProcessingResult Tests
    // ========================================================================

    @Test
    fun `result - Ignored carries reason string`() {
        val result = NotificationProcessingResult.Ignored("No content")
        assertEquals("No content", result.reason)
    }

    @Test
    fun `result - Received carries notification data`() {
        val data = createNotificationData(title = "Test", text = "Content")
        val result = NotificationProcessingResult.Received(data)
        assertEquals(data, result.data)
    }

    @Test
    fun `result - Error carries message and optional data`() {
        val data = createNotificationData(title = "Test", text = "Content")
        val result = NotificationProcessingResult.Error("Parse failed", data)
        assertEquals("Parse failed", result.message)
        assertEquals(data, result.data)
    }

    @Test
    fun `result - Error without data carries null data`() {
        val result = NotificationProcessingResult.Error("Unknown error")
        assertEquals("Unknown error", result.message)
        assertNull(result.data)
    }

    @Test
    fun `result - PendingProcessing carries notification data`() {
        val data = createNotificationData(title = "Test", text = "Content")
        val result = NotificationProcessingResult.PendingProcessing(data)
        assertEquals(data, result.data)
    }

    // ========================================================================
    // Edge Case Tests
    // ========================================================================

    @Test
    fun `edge - notification with empty packageName is still processable`() = runTest {
        val data = createNotificationData(
            packageName = "",
            title = "Alert",
            text = "₹100 debited"
        )

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
        assertEquals("", (result as NotificationProcessingResult.Received).data.packageName)
    }

    @Test
    fun `edge - notification with very long text is processable`() = runTest {
        val longText = "₹" + "0".repeat(10000) + " debited from your account"
        val data = createNotificationData(text = longText)

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `edge - notification with special characters is processable`() = runTest {
        val data = createNotificationData(
            title = "🏦 Bank Alert",
            text = "₹1,250.50 debited • Ref: TXN/2024/ABC-123"
        )

        val result = processor.process(data)
        assertTrue(result is NotificationProcessingResult.Received)
    }

    @Test
    fun `edge - multiple sequential notifications are each processed independently`() = runTest {
        val data1 = createNotificationData(text = "₹100 debited")
        val data2 = createNotificationData(text = "₹200 debited")
        val data3 = createNotificationData(title = null, text = null) // empty

        val result1 = processor.process(data1)
        val result2 = processor.process(data2)
        val result3 = processor.process(data3)

        assertTrue(result1 is NotificationProcessingResult.Received)
        assertTrue(result2 is NotificationProcessingResult.Received)
        assertTrue(result3 is NotificationProcessingResult.Ignored)
    }

    @Test
    fun `edge - notification with notification key is preserved`() {
        val data = createNotificationData(
            title = "Alert",
            text = "₹100 debited",
            notificationKey = "com.sbi|12345"
        )
        assertEquals("com.sbi|12345", data.notificationKey)
    }

    // ========================================================================
    // Service State Tests (static companion)
    // ========================================================================

    @Test
    fun `service - isListenerConnected defaults to false`() {
        // The static field should default to false when no service is running
        assertFalse(FinlyNotificationListenerService.isListenerConnected)
    }

    // ========================================================================
    // Helper
    // ========================================================================

    private fun createNotificationData(
        packageName: String = "com.test.app",
        title: String? = "Test Title",
        text: String? = "Test text content",
        subText: String? = null,
        bigText: String? = null,
        notificationKey: String? = null
    ): RawNotificationData {
        return RawNotificationData(
            packageName = packageName,
            title = title,
            text = text,
            subText = subText,
            bigText = bigText,
            receivedAt = Instant.now(),
            notificationKey = notificationKey
        )
    }
}

