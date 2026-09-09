package com.personalexpensetracker.data.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import com.personalexpensetracker.data.repository.IncomeRepositoryImpl
import com.personalexpensetracker.domain.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class DefaultMessagingBankFilterTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseRepository: ExpenseRepositoryImpl
    private lateinit var incomeRepository: IncomeRepositoryImpl
    private lateinit var processor: AutoExpenseCaptureProcessor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
        incomeRepository = IncomeRepositoryImpl(database.incomeDao())
        processor = AutoExpenseCaptureProcessor(
            expenseRepository = expenseRepository,
            incomeRepository = incomeRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `Google Messages notification with TRAI bank header creates expense`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = "Your A/c ending 4567 debited for INR 450.00 on 09-Sep-26 towards Swiggy. Ref: UPI112233. Avl Bal: INR 12,000",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "g_msg_1"
        )

        val result = processor.process(notif)
        assertTrue("Expected ExpenseCreated but got $result", result is NotificationProcessingResult.ExpenseCreated)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("450.00"), expenses[0].amount)
        assertEquals("Swiggy", expenses[0].title)
        assertEquals(Category.FOOD, expenses[0].category)
    }

    @Test
    fun `Samsung Messages notification with TRAI bank credit header creates income`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.samsung.android.messaging",
            title = "VM-SBIINB",
            text = "INR 25,000.00 credited to A/c ending 4567 on 09-Sep-26 from Acme Corp. Ref: UPI998877. Avl Bal: INR 37,000",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "s_msg_1"
        )

        val result = processor.process(notif)
        assertTrue("Expected IncomeCreated but got $result", result is NotificationProcessingResult.IncomeCreated)

        val incomes = incomeRepository.getAllIncomes().first()
        assertEquals(1, incomes.size)
        assertEquals(BigDecimal("25000.00"), incomes[0].amount)
        assertEquals("Acme Corp", incomes[0].title)
        assertEquals("Bank SMS", incomes[0].source)
    }

    @Test
    fun `Xiaomi MIUI MMS notification with non-hyphenated bank header creates expense`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.miui.mms",
            title = "HDFCBK",
            text = "Rs 1,200.00 debited from A/c 1234 for Amazon. Avl Bal Rs 50,000",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "mi_msg_1"
        )

        val result = processor.process(notif)
        assertTrue("Expected ExpenseCreated but got $result", result is NotificationProcessingResult.ExpenseCreated)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("1200.00"), expenses[0].amount)
        assertEquals("Amazon", expenses[0].title)
        assertEquals(Category.SHOPPING, expenses[0].category)
    }

    @Test
    fun `Personal chat message from friend in Google Messages is IGNORED`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "Rahul Sharma",
            text = "Hey, I sent you 500 rs yesterday, please check",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "chat_msg_1"
        )

        val result = processor.process(notif)
        assertTrue("Personal chat must be Ignored but got $result", result is NotificationProcessingResult.Ignored)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, expenses.size)
    }

    @Test
    fun `Personal group chat in default messaging app is IGNORED`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "Trip to Goa Group",
            text = "Total cost was 15000 rs each",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "group_msg_1"
        )

        val result = processor.process(notif)
        assertTrue("Group chat must be Ignored but got $result", result is NotificationProcessingResult.Ignored)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, expenses.size)
    }

    @Test
    fun `OTP message from bank in Google Messages is IGNORED`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.google.android.apps.messaging",
            title = "VK-HDFCBK",
            text = "Your OTP for transaction of Rs. 2,000.00 at Flipkart is 987654. Do not share OTP with anyone.",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "otp_msg_1"
        )

        val result = processor.process(notif)
        assertTrue("OTP must be Ignored but got $result", result is NotificationProcessingResult.Ignored)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(0, expenses.size)
    }

    @Test
    fun `Direct UPI app notification without SMS package is processed normally`() = runTest {
        val notif = RawNotificationData(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Paid ₹320 to Starbucks successfully",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "phonepe_1"
        )

        val result = processor.process(notif)
        assertTrue("UPI app notification should create expense", result is NotificationProcessingResult.ExpenseCreated)

        val expenses = expenseRepository.getAllExpenses().first()
        assertEquals(1, expenses.size)
        assertEquals(BigDecimal("320.00"), expenses[0].amount)
        assertEquals("Starbucks", expenses[0].title)
        assertEquals(Category.FOOD, expenses[0].category)
    }
}
