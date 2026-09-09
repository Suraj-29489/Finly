package com.personalexpensetracker.data.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
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
class AutoExpenseCapturePipelineIntegrationTest {

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
        processor = AutoExpenseCaptureProcessor(expenseRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `pipeline - valid debit notification creates expense in Room database`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            text = "₹450 debited from A/c XXXX1234 for Swiggy. Ref: UPI987654",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_1"
        )

        val result = processor.process(notification)

        assertTrue("Expected ExpenseCreated result but got $result", result is NotificationProcessingResult.ExpenseCreated)
        val created = result as NotificationProcessingResult.ExpenseCreated
        assertEquals(Category.FOOD, created.category)
        assertEquals(BigDecimal("450.00"), created.parsedTransaction.amount)
        assertEquals("Swiggy", created.parsedTransaction.merchant)

        // Verify persisted in Room database through ExpenseRepository
        val allExpenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, allExpenses.size)
        val expense = allExpenses[0]
        assertEquals("Swiggy", expense.title)
        assertEquals(BigDecimal("450.00"), expense.amount)
        assertEquals(Category.FOOD, expense.category)
    }

    @Test
    fun `pipeline - credit notification is ignored and does NOT create expense`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            text = "₹50,000 credited to your account XXXX5678. Salary for August.",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_2"
        )

        val result = processor.process(notification)

        assertTrue("Expected CreditIgnored result but got $result", result is NotificationProcessingResult.CreditIgnored)
        val allExpenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, allExpenses.size)
    }

    @Test
    fun `pipeline - OTP notification is ignored and does NOT create expense`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "Bank OTP",
            text = "Your OTP is 482913 for transaction of Rs 1200 at Amazon. Do not share.",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_3"
        )

        val result = processor.process(notification)

        assertTrue(result is NotificationProcessingResult.Ignored || result is NotificationProcessingResult.InvalidTransaction)
        val allExpenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, allExpenses.size)
    }

    @Test
    fun `pipeline - duplicate notification creates only one expense`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "GPay",
            text = "Payment of ₹250 to Zomato was successful. Ref: GPAY12345",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_gpay_1"
        )

        val result1 = processor.process(notification)
        val result2 = processor.process(notification)

        assertTrue(result1 is NotificationProcessingResult.ExpenseCreated)
        assertTrue(result2 is NotificationProcessingResult.Duplicate)

        val allExpenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, allExpenses.size)
    }

    @Test
    fun `pipeline - multiple distinct debit notifications create multiple expenses with correct categories`() = runTest {
        val notifFood = RawNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI",
            text = "₹350 debited for Domino's Pizza",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "n_food"
        )
        val notifTravel = RawNotificationData(
            packageName = "com.hdfc.retail.banking",
            title = "HDFC",
            text = "₹480 paid to Uber for cab ride",
            subText = null,
            bigText = null,
            receivedAt = Instant.now().plusSeconds(10),
            notificationKey = "n_travel"
        )
        val notifShopping = RawNotificationData(
            packageName = "com.axis.mobile",
            title = "Axis Bank",
            text = "₹2,499 debited for Amazon purchase",
            subText = null,
            bigText = null,
            receivedAt = Instant.now().plusSeconds(20),
            notificationKey = "n_shop"
        )

        val r1 = processor.process(notifFood)
        val r2 = processor.process(notifTravel)
        val r3 = processor.process(notifShopping)

        assertTrue(r1 is NotificationProcessingResult.ExpenseCreated)
        assertTrue(r2 is NotificationProcessingResult.ExpenseCreated)
        assertTrue(r3 is NotificationProcessingResult.ExpenseCreated)

        val allExpenses = expenseRepository.getAllExpenses().first()
        assertEquals(3, allExpenses.size)

        val foodExp = allExpenses.find { it.category == Category.FOOD }
        val travelExp = allExpenses.find { it.category == Category.TRANSPORTATION }
        val shopExp = allExpenses.find { it.category == Category.SHOPPING }

        assertNotNull(foodExp)
        assertNotNull(travelExp)
        assertNotNull(shopExp)
    }

    @Test
    fun `pipeline - unknown merchant falls back to OTHER category safely`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI",
            text = "₹100 debited from your account at ABCXYZ9999",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_other"
        )

        val result = processor.process(notification)
        assertTrue(result is NotificationProcessingResult.ExpenseCreated)
        val created = result as NotificationProcessingResult.ExpenseCreated
        assertEquals(Category.OTHER, created.category)
    }

    @Test
    fun `pipeline - sent with intervening amount creates expense`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "Sent ₹500 to Ramesh via GPay",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_sent_ramesh"
        )

        val result = processor.process(notification)
        assertTrue("Expected ExpenseCreated but got $result", result is NotificationProcessingResult.ExpenseCreated)
        val created = result as NotificationProcessingResult.ExpenseCreated
        assertEquals(BigDecimal("500.00"), created.parsedTransaction.amount)
        assertEquals("Ramesh", created.parsedTransaction.merchant)
    }

    @Test
    fun `pipeline - notification with leading balance creates expense with debit amount, not balance`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.hdfc.retail.banking",
            title = "HDFC Bank Alert",
            text = "Avl Bal: INR 25,000.00. Your A/c has been debited for INR 450.00 for Swiggy",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_bal_lead"
        )

        val result = processor.process(notification)
        assertTrue("Expected ExpenseCreated but got $result", result is NotificationProcessingResult.ExpenseCreated)
        val created = result as NotificationProcessingResult.ExpenseCreated
        assertEquals(BigDecimal("450.00"), created.parsedTransaction.amount)
        assertEquals("Swiggy", created.parsedTransaction.merchant)
        assertEquals(Category.FOOD, created.category)
    }

    @Test
    fun `pipeline - UPI VPA payee creates expense with correct merchant and category`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Paid ₹250 to swiggy@icici successfully",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_upi_vpa"
        )

        val result = processor.process(notification)
        assertTrue("Expected ExpenseCreated but got $result", result is NotificationProcessingResult.ExpenseCreated)
        val created = result as NotificationProcessingResult.ExpenseCreated
        assertEquals(BigDecimal("250.00"), created.parsedTransaction.amount)
        assertEquals("swiggy@icici", created.parsedTransaction.merchant)
        assertEquals(Category.FOOD, created.category)
    }

    @Test
    fun `pipeline - merchant in title and amount in body creates expense`() = runTest {
        val notification = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Swiggy",
            text = "Paid ₹450 using UPI",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "notif_title_merchant"
        )

        val result = processor.process(notification)
        assertTrue("Expected ExpenseCreated but got $result", result is NotificationProcessingResult.ExpenseCreated)
        val created = result as NotificationProcessingResult.ExpenseCreated
        assertEquals(BigDecimal("450.00"), created.parsedTransaction.amount)
        assertEquals("Swiggy", created.parsedTransaction.merchant)
        assertEquals(Category.FOOD, created.category)
    }
}

