package com.personalexpensetracker.data.notification.duplicate

import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import com.personalexpensetracker.data.notification.parser.ParsedTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class DuplicateTransactionDetectorTest {

    private lateinit var detector: DuplicateTransactionDetector

    @Before
    fun setUp() {
        detector = DuplicateTransactionDetector()
    }

    @Test
    fun `isDuplicate - returns true when reference ID matches`() {
        val txn1 = createTxn(amount = "450.00", merchant = "Swiggy", refId = "REF_12345", key = "key_1")
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "450.00", merchant = "Swiggy", refId = "REF_12345", key = "key_2")
        assertTrue(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns true when notification key matches`() {
        val txn1 = createTxn(amount = "450.00", merchant = "Swiggy", refId = null, key = "notif_key_sbi")
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "450.00", merchant = "Swiggy", refId = null, key = "notif_key_sbi")
        assertTrue(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns true for same amount and merchant within time tolerance`() {
        val now = Instant.now()
        val txn1 = createTxn(amount = "250.00", merchant = "Zomato", refId = null, key = null, time = now)
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "250.00", merchant = "Zomato", refId = null, key = null, time = now.plusSeconds(30))
        assertTrue(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns false for different amounts`() {
        val now = Instant.now()
        val txn1 = createTxn(amount = "250.00", merchant = "Zomato", time = now)
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "350.00", merchant = "Zomato", time = now)
        assertFalse(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns false for different merchants`() {
        val now = Instant.now()
        val txn1 = createTxn(amount = "250.00", merchant = "Zomato", time = now)
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "250.00", merchant = "Swiggy", time = now)
        assertFalse(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns true for same transaction within 60 min email delivery delay`() {
        val now = Instant.now()
        val txn1 = createTxn(amount = "250.00", merchant = "Zomato", time = now.minusSeconds(900)) // 15 mins ago (Email sync)
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "250.00", merchant = "Zomato", time = now)
        assertTrue(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns true for normalized reference ID variations`() {
        val txn1 = createTxn(amount = "500.00", merchant = "Swiggy", refId = "UPI1234567890")
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "500.00", merchant = "Swiggy", refId = "1234567890")
        assertTrue(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns true for same amount and accountLast4 across different merchants or bank titles`() {
        val now = Instant.now()
        val txn1 = createTxn(amount = "350.00", merchant = "Starbucks", accountLast4 = "1234", time = now.minusSeconds(600))
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "350.00", merchant = "HDFC Bank Alerts", accountLast4 = "1234", time = now)
        assertTrue(detector.isDuplicate(txn2))
    }

    @Test
    fun `isDuplicate - returns false for same transaction outside 60 min time tolerance`() {
        val now = Instant.now()
        val txn1 = createTxn(amount = "250.00", merchant = "Zomato", time = now.minusSeconds(4500)) // 75 mins ago
        detector.recordTransaction(txn1)

        val txn2 = createTxn(amount = "250.00", merchant = "Zomato", time = now)
        assertFalse(detector.isDuplicate(txn2))
    }

    private fun createTxn(
        amount: String,
        merchant: String,
        refId: String? = null,
        key: String? = null,
        accountLast4: String? = null,
        time: Instant = Instant.now()
    ): ParsedTransaction {
        return ParsedTransaction(
            amount = BigDecimal(amount),
            merchant = merchant,
            direction = TransactionDirection.DEBIT,
            transactionTime = time,
            sourcePackage = "com.test.app",
            referenceId = refId,
            notificationKey = key,
            accountLast4 = accountLast4
        )
    }
}

