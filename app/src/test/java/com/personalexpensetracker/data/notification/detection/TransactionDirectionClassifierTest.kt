package com.personalexpensetracker.data.notification.detection

import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDirectionClassifierTest {

    @Test
    fun `classify - identifies clear debit transactions`() {
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("₹450 debited from A/c XXXX1234 for Swiggy"))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("Payment of ₹250 to Zomato successful"))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("Paid Rs 1200 at Amazon using UPI"))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("₹3,500 spent on HDFC credit card ending 1234"))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("Amount deducted: INR 500 for Uber ride"))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("Cash withdrawn: Rs 2000 from SBI ATM"))
        assertEquals(TransactionDirection.DEBIT, TransactionDirectionClassifier.classify("Bill payment of ₹850 to Airtel successful"))
    }

    @Test
    fun `classify - identifies clear credit transactions`() {
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify("₹50,000 credited to your account XXXX5678"))
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify("Salary credited: Rs 80,000 for August 2026"))
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify("Refund received: ₹450 from Swiggy"))
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify("Cash deposited: INR 10,000 in your savings A/c"))
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify("Money received: ₹1,500 from John via PhonePe"))
        assertEquals(TransactionDirection.CREDIT, TransactionDirectionClassifier.classify("Cashback credited: ₹25 on your recent purchase"))
    }

    @Test
    fun `classify - handles failed and declined transactions safely as UNKNOWN`() {
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("Payment of ₹450 to Swiggy failed"))
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("Transaction of Rs 1200 was declined"))
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("UPI payment of ₹250 cancelled by user"))
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("Debit of Rs 500 reversed"))
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("Payment is pending approval"))
    }

    @Test
    fun `classify - returns UNKNOWN for empty or non-directional text`() {
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify(""))
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("Hello your balance is ₹5000"))
        assertEquals(TransactionDirection.UNKNOWN, TransactionDirectionClassifier.classify("Random text without payment signals"))
    }
}

