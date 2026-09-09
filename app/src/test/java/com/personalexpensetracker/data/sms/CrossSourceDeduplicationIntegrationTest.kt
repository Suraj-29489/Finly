package com.personalexpensetracker.data.sms

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.AutoExpenseCapturePipeline
import com.personalexpensetracker.data.notification.AutoExpenseCaptureProcessor
import com.personalexpensetracker.data.notification.duplicate.DuplicateTransactionDetector
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import com.personalexpensetracker.data.repository.IncomeRepositoryImpl
import com.personalexpensetracker.domain.model.Category
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CrossSourceDeduplicationIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseRepository: ExpenseRepositoryImpl
    private lateinit var duplicateDetector: DuplicateTransactionDetector
    private lateinit var processor: AutoExpenseCaptureProcessor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
        duplicateDetector = DuplicateTransactionDetector()
        processor = AutoExpenseCaptureProcessor(expenseRepository, duplicateDetector)
        AutoExpenseCapturePipeline.setProcessorForTesting(processor)
    }

    @After
    fun tearDown() {
        AutoExpenseCapturePipeline.setProcessorForTesting(null)
        database.close()
    }

    @Test
    fun `cross source - push notification followed by SMS for same transaction creates only one expense`() = runTest {
        val now = Instant.now()

        // 1. Push notification from GPay
        val pushNotif = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Payment of ₹450 to Swiggy was successful. UPI Ref: UPI778899",
            subText = null,
            bigText = null,
            receivedAt = now,
            notificationKey = "notif_push_1"
        )

        // 2. Bank SMS arrives 10 seconds later for the same transaction
        val bankSms = RawNotificationData(
            packageName = "sms",
            title = "VK-HDFCBK",
            text = "Your A/c XXXX1234 has been debited for INR 450.00 on 08-Sep-26 towards Swiggy. Ref: UPI778899. Avl Bal: INR 35,000",
            subText = "SMS",
            bigText = null,
            receivedAt = now.plusSeconds(10),
            notificationKey = "sms_hdfc_1"
        )

        val result1 = processor.process(pushNotif)
        assertTrue("Push notification should create expense", result1 is NotificationProcessingResult.ExpenseCreated)

        val result2 = processor.process(bankSms)
        assertTrue("SMS should be detected as duplicate and not create a second expense", result2 is NotificationProcessingResult.Duplicate)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("450.00"), expenses[0].amount)
        assertEquals("Swiggy", expenses[0].title)
        assertEquals(Category.FOOD, expenses[0].category)
    }

    @Test
    fun `cross source - SMS followed by push notification without ref ID uses amount, merchant, and time proximity`() = runTest {
        val now = Instant.now()

        // 1. Bank SMS arrives first
        val bankSms = RawNotificationData(
            packageName = "sms",
            title = "AD-SBIINB",
            text = "Rs 250.00 debited from A/c 5678 for Zomato",
            subText = "SMS",
            bigText = null,
            receivedAt = now,
            notificationKey = "sms_sbi_1"
        )

        // 2. App notification arrives 15 seconds later
        val appNotif = RawNotificationData(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Paid Rs. 250 to Zomato successfully",
            subText = null,
            bigText = null,
            receivedAt = now.plusSeconds(15),
            notificationKey = "notif_phonepe_1"
        )

        val r1 = processor.process(bankSms)
        assertTrue("SMS should create expense", r1 is NotificationProcessingResult.ExpenseCreated)

        val r2 = processor.process(appNotif)
        assertTrue("App notification within 2 minutes for same amount and merchant is a duplicate", r2 is NotificationProcessingResult.Duplicate)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("250.00"), expenses[0].amount)
        assertEquals("Zomato", expenses[0].title)
    }

    @Test
    fun `SMS ingestion - valid bank debit SMS creates expense with correct merchant and category`() = runTest {
        val sms = RawNotificationData(
            packageName = "sms",
            title = "VM-ICICIB",
            text = "Dear Customer, your Acct ending 4321 is debited with INR 1,200.00 on 08-Sep-26 for Amazon. Available Balance is INR 18,500.00",
            subText = "SMS",
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_icici_1"
        )

        val result = processor.process(sms)
        assertTrue("Expected ExpenseCreated result but got $result", result is NotificationProcessingResult.ExpenseCreated)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("1200.00"), expenses[0].amount)
        assertEquals("Amazon", expenses[0].title)
        assertEquals(Category.SHOPPING, expenses[0].category)
    }

    @Test
    fun `SMS ingestion - credit SMS is ignored and creates 0 expenses`() = runTest {
        val sms = RawNotificationData(
            packageName = "sms",
            title = "VK-HDFCBK",
            text = "INR 50,000.00 credited to A/c ending 1234 on 01-Sep-26. Info: SALARY FOR AUGUST. Avl Bal: INR 65,000.00",
            subText = "SMS",
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_salary_1"
        )

        val result = processor.process(sms)
        assertTrue("Expected CreditIgnored result but got $result", result is NotificationProcessingResult.CreditIgnored)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, expenses.size)
    }

    @Test
    fun `SMS ingestion - OTP message is ignored and creates 0 expenses`() = runTest {
        val sms = RawNotificationData(
            packageName = "sms",
            title = "VK-HDFCBK",
            text = "Your OTP for transaction of INR 1,500.00 at Flipkart is 987654. Do not share OTP with anyone.",
            subText = "SMS",
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_otp_1"
        )

        val result = processor.process(sms)
        assertTrue(result is NotificationProcessingResult.Ignored || result is NotificationProcessingResult.InvalidTransaction)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, expenses.size)
    }

    @Test
    fun `SMS ingestion - credit SMS with incomeRepository creates income record in database`() = runTest {
        val incomeRepository = IncomeRepositoryImpl(database.incomeDao())
        val processorWithIncome = AutoExpenseCaptureProcessor(
            expenseRepository = expenseRepository,
            duplicateDetector = duplicateDetector,
            incomeRepository = incomeRepository
        )

        val sms = RawNotificationData(
            packageName = "sms",
            title = "VK-HDFCBK",
            text = "INR 50,000.00 credited to A/c ending 1234 on 01-Sep-26. Info: SALARY FOR AUGUST. Avl Bal: INR 65,000.00",
            subText = "SMS",
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "sms_salary_1"
        )

        val result = processorWithIncome.process(sms)
        assertTrue("Expected IncomeCreated result but got $result", result is NotificationProcessingResult.IncomeCreated)

        val created = result as NotificationProcessingResult.IncomeCreated
        assertEquals(BigDecimal("50000.00"), created.parsedTransaction.amount)
        assertEquals("SALARY", created.parsedTransaction.merchant)

        val incomes = incomeRepository.getAllIncomes().first()
        assertEquals(1, incomes.size)
        assertEquals(BigDecimal("50000.00"), incomes[0].amount)
        assertEquals("SALARY", incomes[0].title)
        assertEquals("SMS", incomes[0].source)

        // Verify duplicate detection prevents second income insertion
        val dupResult = processorWithIncome.process(sms)
        assertTrue("Duplicate credit SMS should be recognized as Duplicate", dupResult is NotificationProcessingResult.Duplicate)
        assertEquals(1, incomeRepository.getAllIncomes().first().size)
    }

    @Test
    fun `cross source - bank SMS followed by Gmail notification 15 minutes later creates only one expense`() = runTest {
        val now = Instant.now()

        // 1. Text SMS from default messaging app arrives first
        val bankSms = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = "Alert! Rs 500.00 debited from HDFC Bank A/c xx1234 on 09-SEP-26 towards SWIGGY. Avl bal: Rs 15000. Ref: 425318920192.",
            subText = null,
            bigText = null,
            receivedAt = now,
            notificationKey = "sms_hdfc_500"
        )

        // 2. Email notification arrives 15 minutes later via Gmail
        val bankEmail = RawNotificationData(
            packageName = "com.google.android.gm",
            title = "HDFC Bank InstaAlerts",
            text = "Dear Customer, INR 500.00 has been debited from your account ending 1234 towards SWIGGY on 09-Sep-2026. Ref no. 425318920192. Available Balance is Rs. 15,000.00.",
            subText = null,
            bigText = null,
            receivedAt = now.plusSeconds(900), // 15 mins later
            notificationKey = "email_hdfc_500"
        )

        val r1 = processor.process(bankSms)
        assertTrue("SMS should create expense", r1 is NotificationProcessingResult.ExpenseCreated)

        val r2 = processor.process(bankEmail)
        assertTrue("Email notification 15 mins later should be recognized as Duplicate", r2 is NotificationProcessingResult.Duplicate)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals("Database must contain exactly one expense", 1, expenses.size)
        assertEquals(BigDecimal("500.00"), expenses[0].amount)
        assertEquals("SWIGGY", expenses[0].title)
    }

    @Test
    fun `cross source - bank email arrives first with generic title, subsequent SMS enriches merchant and deduplicates`() = runTest {
        val now = Instant.now()

        // 1. Email arrives first with generic bank title in body
        val bankEmail = RawNotificationData(
            packageName = "com.google.android.gm",
            title = "HDFC Bank Alerts",
            text = "Debit Alert: INR 350.00 spent on A/c ending 1234. Avl Bal Rs 20,000.",
            subText = null,
            bigText = null,
            receivedAt = now,
            notificationKey = "email_hdfc_350"
        )

        // 2. SMS arrives 2 minutes later with specific merchant
        val bankSms = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = "Rs 350.00 debited from A/c 1234 on 09-09-26 towards Starbucks. Ref 998877.",
            subText = null,
            bigText = null,
            receivedAt = now.plusSeconds(120),
            notificationKey = "sms_hdfc_350"
        )

        val r1 = processor.process(bankEmail)
        assertTrue("Initial email should create expense", r1 is NotificationProcessingResult.ExpenseCreated)

        val r2 = processor.process(bankSms)
        assertTrue("Subsequent SMS should be recognized as duplicate", r2 is NotificationProcessingResult.Duplicate)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("350.00"), expenses[0].amount)
        // Verify title was enriched from generic to Starbucks
        assertEquals("Starbucks", expenses[0].title)
    }

    @Test
    fun `SMS ingestion - credit SMS with complex UPI wording is skipped and creates 0 expenses`() = runTest {
        val sms1 = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = "Dear Customer, your A/c ending 1234 has been credited with Rs 500.00 on 09-09-26 by UPI payment from Ramesh (UPI Ref no 1234567890). Avail bal Rs 5,500.00.",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "credit_sms_1"
        )

        val sms2 = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "AD-SBIINB",
            text = "Payment of INR 1,500.00 received in your account ending 5678 via UPI from John.",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "credit_sms_2"
        )

        val r1 = processor.process(sms1)
        assertTrue("Credit SMS with UPI payment from Ramesh must be skipped", r1 is NotificationProcessingResult.CreditIgnored)

        val r2 = processor.process(sms2)
        assertTrue("Credit SMS with received via UPI must be skipped", r2 is NotificationProcessingResult.CreditIgnored)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals("No expenses must be created for credit messages", 0, expenses.size)
    }

    @Test
    fun `pipeline - negative amount notification creates expense with positive amount`() = runTest {
        val negNotif = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Txn: -1 to Ramesh. Ref: 987654321",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "neg_txn_1"
        )

        val result = processor.process(negNotif)
        assertTrue("Negative amount notification must create an expense", result is NotificationProcessingResult.ExpenseCreated)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("1.00"), expenses[0].amount)
        assertEquals("Ramesh", expenses[0].title)
    }
}
