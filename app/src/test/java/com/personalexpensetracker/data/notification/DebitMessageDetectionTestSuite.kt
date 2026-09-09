package com.personalexpensetracker.data.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier
import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.notification.parser.TransactionParser
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant

/**
 * Test suite evaluating debit message detection:
 * - "sent" variations
 * - negative integer / amount terms ("-1", "-500", "-₹500", etc.)
 * - Indian bank debit notifications & Dr statements
 * - Credit skipping & balance exclusion safety
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DebitMessageDetectionTestSuite {

    private lateinit var database: AppDatabase
    private lateinit var expenseRepository: ExpenseRepositoryImpl
    private lateinit var processor: AutoExpenseCaptureProcessor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
        processor = AutoExpenseCaptureProcessor(
            expenseRepository = expenseRepository,
            incomeRepository = null // Strict debit-only mode
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // SECTION 1: "SENT" KEYWORD TEST CASES
    // =========================================================================

    @Test
    fun `sent - You sent Rs 500 to Ramesh via UPI`() = runTest {
        val text = "You sent ₹500 to Ramesh via UPI"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("500.00"), TransactionParser.extractAmount(text))
        assertEquals("Ramesh", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
        assertEquals(BigDecimal("500.00"), expenseRepository.getAllExpenses().first().first().amount)
    }

    @Test
    fun `sent - Sent Rs 100 to Anita for coffee`() = runTest {
        val text = "Sent Rs. 100 to Anita for coffee"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("100.00"), TransactionParser.extractAmount(text))
        assertEquals("Anita", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "PhonePe", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `sent - Sent Rs 1,250_50 to Zomato`() = runTest {
        val text = "Sent ₹1,250.50 to Zomato"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("1250.50"), TransactionParser.extractAmount(text))
        assertEquals("Zomato", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Paytm", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `sent - Money sent to Swiggy INR 350_00`() = runTest {
        val text = "Money sent to Swiggy: INR 350.00"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("350.00"), TransactionParser.extractAmount(text))
        assertEquals("Swiggy", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "BHIM", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `sent - Sent 200 to Chai Point`() = runTest {
        val text = "Sent 200 to Chai Point"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("200.00"), TransactionParser.extractAmount(text))
        assertEquals("Chai Point", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `sent - Sent -1 to Friend`() = runTest {
        val text = "Sent -1 to Friend"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("1.00"), TransactionParser.extractAmount(text))
        assertEquals("Friend", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    // =========================================================================
    // SECTION 2: NEGATIVE INTEGER / AMOUNT TERMS ("-" TERMS)
    // =========================================================================

    @Test
    fun `negative - Txn -1`() = runTest {
        val text = "Txn: -1"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("1.00"), TransactionParser.extractAmount(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("1.00"), expenses[0].amount)
    }

    @Test
    fun `negative - Ac XX1234 -1_00 to Ramesh`() = runTest {
        val text = "A/c XX1234: -1.00 to Ramesh. Ref: 987654321"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("1.00"), TransactionParser.extractAmount(text))
        assertEquals("Ramesh", TransactionParser.extractMerchant(text))
        assertEquals("987654321", TransactionParser.extractReferenceId(text))
        assertEquals("1234", TransactionParser.extractAccountLast4(text))

        val notif = createNotification(title = "HDFC Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - -Rs 500 spent at Swiggy`() = runTest {
        val text = "-₹500 spent at Swiggy"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("500.00"), TransactionParser.extractAmount(text))
        assertEquals("Swiggy", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Google Pay -50 to Tea Stall`() = runTest {
        val text = "Google Pay: -50 to Tea Stall"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("50.00"), TransactionParser.extractAmount(text))
        assertEquals("Tea Stall", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Axis Bank -Rs 250 paid`() = runTest {
        val text = "Axis Bank: -Rs. 250 paid"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("250.00"), TransactionParser.extractAmount(text))

        val notif = createNotification(title = "Axis Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Rs-120 debited from Ac 5678`() = runTest {
        val text = "₹-120 debited from A/c 5678"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("120.00"), TransactionParser.extractAmount(text))
        assertEquals("5678", TransactionParser.extractAccountLast4(text))

        val notif = createNotification(title = "SBI Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Rs-100 debited for Uber`() = runTest {
        val text = "Rs.-100 debited for Uber"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("100.00"), TransactionParser.extractAmount(text))
        assertEquals("Uber", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "ICICI Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - -INR 1,500 transferred to Landlord`() = runTest {
        val text = "-INR 1,500 transferred to Landlord"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("1500.00"), TransactionParser.extractAmount(text))
        assertEquals("Landlord", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "HDFC Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Payment -250_50 for groceries`() = runTest {
        val text = "Payment: -250.50 for groceries"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("250.50"), TransactionParser.extractAmount(text))
        assertEquals("groceries", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Paytm", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - (-50_00) at Starbucks`() = runTest {
        val text = "(-50.00) at Starbucks"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("50.00"), TransactionParser.extractAmount(text))
        assertEquals("Starbucks", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Google Pay", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Account -500_00`() = runTest {
        val text = "Account: -500.00"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("500.00"), TransactionParser.extractAmount(text))

        val notif = createNotification(title = "Kotak Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `negative - Txn -1000`() = runTest {
        val text = "Txn: -1000"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("1000.00"), TransactionParser.extractAmount(text))

        val notif = createNotification(title = "PhonePe", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    // =========================================================================
    // SECTION 3: REAL INDIAN BANK SMS & PUSH NOTIFICATIONS
    // =========================================================================

    @Test
    fun `bank sms - HDFC debited for INR 450_00 towards Swiggy`() = runTest {
        val text = "Your A/c ending 4567 debited for INR 450.00 on 09-Sep-26 towards Swiggy. Ref: UPI112233. Avl Bal: INR 12,000"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount(text))
        assertEquals("Swiggy", TransactionParser.extractMerchant(text))
        assertEquals("UPI112233", TransactionParser.extractReferenceId(text))
        assertEquals("4567", TransactionParser.extractAccountLast4(text))

        val notif = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = text,
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_hdfc_1"
        )
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - SBI Ac is Dr for Rs 500 at Amazon`() = runTest {
        val text = "Dear SBI User, A/c *4321 is Dr. for Rs 500 on 09-09-26 at Amazon. Avail Bal: Rs 25,000"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("500.00"), TransactionParser.extractAmount(text))
        assertEquals("Amazon", TransactionParser.extractMerchant(text))
        assertEquals("4321", TransactionParser.extractAccountLast4(text))

        val notif = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VM-SBIINB",
            text = text,
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_sbi_1"
        )
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - ICICI Paid Rs 120 at CCD using UPI`() = runTest {
        val text = "Paid Rs 120 at CCD using UPI. Ref: 123456789012"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("120.00"), TransactionParser.extractAmount(text))
        assertEquals("CCD", TransactionParser.extractMerchant(text))
        assertEquals("123456789012", TransactionParser.extractReferenceId(text))

        val notif = createNotification(title = "AD-ICICIB", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - Spent INR 3,500 on HDFC card ending 8899 at Croma`() = runTest {
        val text = "Spent INR 3,500 on HDFC card ending 8899 at Croma. Avl Lmt: INR 50,000"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("3500.00"), TransactionParser.extractAmount(text))
        assertEquals("Croma", TransactionParser.extractMerchant(text))
        assertEquals("8899", TransactionParser.extractAccountLast4(text))

        val notif = createNotification(title = "HDFC Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - Amount deducted Rs 300 for Uber ride`() = runTest {
        val text = "Amount deducted: Rs 300 for Uber ride"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("300.00"), TransactionParser.extractAmount(text))
        assertEquals("Uber ride", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Axis Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - ATM cash withdrawal Rs 2000`() = runTest {
        val text = "Cash withdrawn: Rs 2,000 from SBI ATM"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("2000.00"), TransactionParser.extractAmount(text))

        val notif = createNotification(title = "SBI Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - Bill payment of Rs 850 to Airtel successful`() = runTest {
        val text = "Bill payment of ₹850 to Airtel successful"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("850.00"), TransactionParser.extractAmount(text))
        assertEquals("Airtel", TransactionParser.extractMerchant(text))

        val notif = createNotification(title = "Paytm", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    @Test
    fun `bank sms - Charged Rs 299 on card ending 1234 by merchant Spotify`() = runTest {
        val text = "Charged Rs 299 on card ending 1234 by merchant: Spotify"
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify(text))
        assertEquals(BigDecimal("299.00"), TransactionParser.extractAmount(text))
        assertEquals("Spotify", TransactionParser.extractMerchant(text))
        assertEquals("1234", TransactionParser.extractAccountLast4(text))

        val notif = createNotification(title = "Kotak Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
    }

    // =========================================================================
    // SECTION 4: SAFETY CHECKS (CREDIT SKIPPING & BALANCE EXCLUSION)
    // =========================================================================

    @Test
    fun `safety - incoming transfer to your account is classified as CREDIT and skipped`() = runTest {
        val text = "INR 5,000 transferred to your account ending 1234 from Ramesh. UPI txn ref 123456."
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify(text))

        val notif = createNotification(title = "VK-HDFCBK", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.CreditIgnored)
        assertEquals(0, expenseRepository.getAllExpenses().first().size)
    }

    @Test
    fun `safety - salary credited is classified as CREDIT and skipped`() = runTest {
        val text = "Salary of Rs 50,000 credited to your A/c XX5678 on 01-Sep-26"
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify(text))

        val notif = createNotification(title = "VM-SBIINB", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.CreditIgnored)
        assertEquals(0, expenseRepository.getAllExpenses().first().size)
    }

    @Test
    fun `safety - refund received is classified as CREDIT and skipped`() = runTest {
        val text = "Refund received: ₹450 from Swiggy for cancelled order"
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify(text))

        val notif = createNotification(title = "Swiggy", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.CreditIgnored)
        assertEquals(0, expenseRepository.getAllExpenses().first().size)
    }

    @Test
    fun `safety - overdraft negative balance statement alone does NOT create expense`() = runTest {
        val text = "Dear Customer, Avl Bal: -500.00 on your A/c 1234"
        val parsedAmount = TransactionParser.extractAmount(text)
        // Since the only amount is preceded by "Avl Bal:", it must be ignored as balance
        assertEquals(null, parsedAmount)

        val notif = createNotification(title = "HDFC Bank", text = text)
        val result = processor.process(notif)
        assertTrue(result is NotificationProcessingResult.Ambiguous || result is NotificationProcessingResult.InvalidTransaction || result is NotificationProcessingResult.Ignored)
        assertEquals(0, expenseRepository.getAllExpenses().first().size)
    }

    private fun createNotification(title: String, text: String): RawNotificationData {
        return RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = title,
            text = text,
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "test_key_${System.nanoTime()}"
        )
    }
}
