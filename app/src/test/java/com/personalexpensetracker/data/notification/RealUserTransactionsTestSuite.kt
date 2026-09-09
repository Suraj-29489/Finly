package com.personalexpensetracker.data.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.detection.FinancialNotificationDetector
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RealUserTransactionsTestSuite {

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

    private fun createSmsNotification(title: String, text: String): RawNotificationData {
        return RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = title,
            text = text,
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_test_${System.nanoTime()}"
        )
    }

    // 1. Slice: Rs. 960 sent from a/c xx0322 on 17-Jun-26 to MAHESH COMPANY
    @Test
    fun testRealTransaction_01_MaheshCompany_960() = runTest {
        val title = "VK-SLCBNK-S"
        val text = "Rs. 960 sent from a/c xx0322 on 17-Jun-26 to MAHESH COMPANY (UPI Ref: 616835125961). Not you? Call 08048329999 - slice"

        assertTrue("Should be bank sender", FinancialNotificationDetector.isBankSender(title))
        assertTrue("Should be bank SMS", FinancialNotificationDetector.isSpecificBankSmsNotification(title, text))

        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("960.00"), parsed!!.amount)
        assertEquals("MAHESH COMPANY", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("616835125961", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 2. Slice: Rs. 9.70 sent from a/c xx0322 on 27-Jun-26 to Indian Railways UTS
    @Test
    fun testRealTransaction_02_IndianRailways_9_70() = runTest {
        val title = "VK-SLCBNK-S"
        val text = "Rs. 9.70 sent from a/c xx0322 on 27-Jun-26 to Indian Railways UTS (UPI Ref: 654474188225). Not you? Call 08048329999 - slice"

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        assertTrue(FinancialNotificationDetector.isSpecificBankSmsNotification(title, text))

        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("9.70"), parsed!!.amount)
        assertEquals("Indian Railways UTS", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("654474188225", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 3. Slice: Rs. 4.85 sent from a/c xx0322 on 09-Sep-26 to Indian Railways UTS
    @Test
    fun testRealTransaction_03_IndianRailways_4_85() = runTest {
        val title = "VK-SLCBNK-S"
        val text = "Rs. 4.85 sent from a/c xx0322 on 09-Sep-26 to Indian Railways UTS (UPI Ref: 129307963204). Not you? Call 08048329999 - slice"

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("4.85"), parsed!!.amount)
        assertEquals("Indian Railways UTS", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("129307963204", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 4. Slice: Rs. 125 sent from a/c xx0322 on 08-Sep-26 to Sourav Store
    @Test
    fun testRealTransaction_04_SouravStore_125() = runTest {
        val title = "VA-SLCBNK-S"
        val text = "Rs. 125 sent from a/c xx0322 on 08-Sep-26 to Sourav Store (UPI Ref: 625159788516). Not you? Call 08048329999 - slice"

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("125.00"), parsed!!.amount)
        assertEquals("Sourav Store", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("625159788516", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 5. Slice: Rs. 1,000 sent from a/c xx0322 on 08-Sep-26 to TECHNO INDIA HOOGHLY
    @Test
    fun testRealTransaction_05_TechnoIndia_1000() = runTest {
        val title = "VA-SLCBNK-S"
        val text = "Rs. 1,000 sent from a/c xx0322 on 08-Sep-26 to TECHNO INDIA HOOGHLY (UPI Ref: 625187723190). Not you? Call 08048329999 - slice"

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("1000.00"), parsed!!.amount)
        assertEquals("TECHNO INDIA HOOGHLY", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("625187723190", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 6. Slice: Rs. 100 sent from a/c xx0322 on 03-Aug-26 to AAMAR KOLKATA METRO
    @Test
    fun testRealTransaction_06_AamarKolkataMetro_100() = runTest {
        val title = "JM-SLCBNK-S"
        val text = "Rs. 100 sent from a/c xx0322 on 03-Aug-26 to AAMAR KOLKATA METRO (UPI Ref: 103786402142). Not you? Call 08048329999 - slice"

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("100.00"), parsed!!.amount)
        assertEquals("AAMAR KOLKATA METRO", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("103786402142", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 7. Slice: Rs. 50 sent from a/c xx0322 on 06-Sep-26 to Sankar Biswas
    @Test
    fun testRealTransaction_07_SankarBiswas_50() = runTest {
        val title = "JM-SLCBNK-S"
        val text = "Rs. 50 sent from a/c xx0322 on 06-Sep-26 to Sankar Biswas (UPI Ref: 624938074811). Not you? Call 08048329999 - slice"

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("50.00"), parsed!!.amount)
        assertEquals("Sankar Biswas", parsed.merchant)
        assertEquals("0322", parsed.accountLast4)
        assertEquals("624938074811", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 8. HDFC: Sent Rs.1.00 From HDFC Bank A/C *7201 To ARYA MUKHERJEE
    @Test
    fun testRealTransaction_08_HdfcUpi_AryaMukherjee_1() = runTest {
        val title = "JX-HDFCBK-S"
        val text = """
            Sent Rs.1.00
            From HDFC Bank A/C
            *7201
            To ARYA MUKHERJEE
            On 13/08/26
            Ref 127857047659
            Not You?
            Call 18002586161/
            SMS BLOCK UPI to
            7308080808
        """.trimIndent()

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        assertTrue(FinancialNotificationDetector.isSpecificBankSmsNotification(title, text))

        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("1.00"), parsed!!.amount)
        assertEquals("ARYA MUKHERJEE", parsed.merchant)
        assertEquals("7201", parsed.accountLast4)
        assertEquals("127857047659", parsed.referenceId)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 9. HDFC: Spent Rs.74 On HDFC Bank Card 6446 At ASSPL
    @Test
    fun testRealTransaction_09_HdfcCard_Asspl_74() = runTest {
        val title = "JX-HDFCBK-S"
        val text = """
            Spent Rs.74 On
            HDFC Bank Card
            6446 At ASSPL On
            2026-09-08:01:09:36.Not
            You? To Block+Reissue
            Call 18002586161/SMS
            BLOCK CC 6446 to
            7308080808
        """.trimIndent()

        assertTrue(FinancialNotificationDetector.isBankSender(title))
        assertTrue(FinancialNotificationDetector.isSpecificBankSmsNotification(title, text))

        val direction = TransactionDirectionClassifier.classify(text)
        assertEquals(TransactionDirection.DEBIT, direction)

        val parsed = TransactionParser.parse(createSmsNotification(title, text), direction)
        assertNotNull(parsed)
        assertEquals(BigDecimal("74.00"), parsed!!.amount)
        assertEquals("ASSPL", parsed.merchant)
        assertEquals("6446", parsed.accountLast4)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue("Expected ExpenseCreated, got $result", result is NotificationProcessingResult.ExpenseCreated)
    }

    // 10. Non-transaction Slice: Weekly saver created (Must be skipped)
    @Test
    fun testRealTransaction_10_SliceSaverCreated_Ignored() = runTest {
        val title = "VA-SLCBNK-S"
        val text = "Your Weekly saver atom was created on 03 Sep '26. Auto-save is set for every Sunday. For queries, call 080-4832-9999 - slice"

        val isFinancial = FinancialNotificationDetector.isFinancialNotification(text)
        assertFalse("Non-financial alert must be false", isFinancial)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue(result is NotificationProcessingResult.Ignored)
        assertEquals(0, expenseRepository.getAllExpenses().first().size)
    }

    // 11. Non-transaction Slice: Weekly saver deleted (Must be skipped)
    @Test
    fun testRealTransaction_11_SliceSaverDeleted_Ignored() = runTest {
        val title = "JM-SLCBNK-S"
        val text = "Your Weekly saver atom has been successfully deleted on 05 Sep '26. For queries, call 080-4832-9999 - slice"

        val isFinancial = FinancialNotificationDetector.isFinancialNotification(text)
        assertFalse("Non-financial alert must be false", isFinancial)

        val result = processor.process(createSmsNotification(title, text))
        assertTrue(result is NotificationProcessingResult.Ignored)
        assertEquals(0, expenseRepository.getAllExpenses().first().size)
    }
}
