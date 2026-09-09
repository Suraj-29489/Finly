package com.personalexpensetracker.data.notification.parser

import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import com.personalexpensetracker.data.notification.model.RawNotificationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class TransactionParserTest {

    @Test
    fun `extractAmount - parses various currency prefixes and formats`() {
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount("₹450 debited"))
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount("Rs 450 debited"))
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount("Rs. 450 debited"))
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount("INR 450 debited"))
        assertEquals(BigDecimal("1250.50"), TransactionParser.extractAmount("₹1,250.50 paid"))
        assertEquals(BigDecimal("25000.00"), TransactionParser.extractAmount("Rs. 25,000 paid"))
        assertEquals(BigDecimal("75.25"), TransactionParser.extractAmount("INR 75.25 charged"))
    }

    @Test
    fun `extractAmount - parses suffix currency formats`() {
        assertEquals(BigDecimal("1250.00"), TransactionParser.extractAmount("Paid 1,250 INR to store"))
        assertEquals(BigDecimal("500.00"), TransactionParser.extractAmount("Debited 500 rupees"))
    }

    @Test
    fun `extractAmount - rejects pure OTP numbers`() {
        assertNull(TransactionParser.extractAmount("Your OTP is 482913 for login"))
        assertNull(TransactionParser.extractAmount("Your verification code: 123456"))
    }

    @Test
    fun `extractMerchant - extracts merchant from common patterns`() {
        assertEquals("Swiggy", TransactionParser.extractMerchant("₹450 debited from A/c XXXX1234 for Swiggy"))
        assertEquals("Zomato", TransactionParser.extractMerchant("Payment of ₹250 to Zomato was successful"))
        assertEquals("Amazon", TransactionParser.extractMerchant("Paid Rs 1200 at Amazon via UPI"))
        assertEquals("Uber", TransactionParser.extractMerchant("₹350 charged by merchant: Uber on card"))
        assertEquals("Flipkart", TransactionParser.extractMerchant("Purchase at Flipkart for ₹2,499"))
        assertEquals("Airtel", TransactionParser.extractMerchant("Payment to Airtel of Rs 599"))
    }

    @Test
    fun `extractReferenceId - extracts reference and transaction IDs`() {
        assertEquals("UPI987654321", TransactionParser.extractReferenceId("₹450 debited. Ref: UPI987654321"))
        assertEquals("TXN123456", TransactionParser.extractReferenceId("Paid ₹250. Txn ID: TXN123456"))
        assertEquals("123456789012", TransactionParser.extractReferenceId("UPI Ref No: 123456789012"))
    }

    @Test
    fun `parse - successfully creates ParsedTransaction from notification`() {
        val raw = RawNotificationData(
            packageName = "com.sbi.lotusintouch",
            title = "SBI Alert",
            text = "₹450 debited from A/c XXXX1234 for Swiggy. Ref: UPI123456",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "key_1"
        )

        val parsed = TransactionParser.parse(raw, TransactionDirection.DEBIT)
        assertNotNull(parsed)
        assertEquals(BigDecimal("450.00"), parsed?.amount)
        assertEquals("Swiggy", parsed?.merchant)
        assertEquals(TransactionDirection.DEBIT, parsed?.direction)
        assertEquals("UPI123456", parsed?.referenceId)
        assertEquals("com.sbi.lotusintouch", parsed?.sourcePackage)
    }

    @Test
    fun `SourceAwareParser - correctly identifies financial sources`() {
        assertEquals(SourceAwareParser.SourceType.BANK, SourceAwareParser.identifySource("com.sbi.lotusintouch"))
        assertEquals(SourceAwareParser.SourceType.BANK, SourceAwareParser.identifySource("com.hdfc.retail.banking"))
        assertEquals(SourceAwareParser.SourceType.UPI, SourceAwareParser.identifySource("com.google.android.apps.nbu.paisa.user"))
        assertEquals(SourceAwareParser.SourceType.UPI, SourceAwareParser.identifySource("net.one97.paytm"))
        assertEquals(SourceAwareParser.SourceType.CARD, SourceAwareParser.identifySource("com.cred.android"))
        assertEquals(SourceAwareParser.SourceType.UNKNOWN, SourceAwareParser.identifySource("com.whatsapp.camera"))
    }

    @Test
    fun `extractAmount - skips available balance and extracts debit amount`() {
        val text1 = "Avl Bal: INR 25,000.00. Your A/c has been debited for INR 450.00"
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount(text1))

        val text2 = "INR 350.00 debited from A/c. Total Bal: INR 12,345.67"
        assertEquals(BigDecimal("350.00"), TransactionParser.extractAmount(text2))

        val text3 = "Available Balance: Rs. 50,000. Amount of Rs. 1,200 spent on Card ending 1234"
        assertEquals(BigDecimal("1200.00"), TransactionParser.extractAmount(text3))
    }

    @Test
    fun `extractAmount - extracts currency-less amount after debit verb`() {
        assertEquals(BigDecimal("450.00"), TransactionParser.extractAmount("A/c XXXX1234 debited by 450.00 on 08Sep26"))
        assertEquals(BigDecimal("1200.00"), TransactionParser.extractAmount("Spent 1200 at Starbucks"))
        assertEquals(BigDecimal("750.50"), TransactionParser.extractAmount("Paid 750.50 to Groceries"))
    }

    @Test
    fun `extractMerchant - handles UPI VPAs and complex payee names`() {
        assertEquals("swiggy@icici", TransactionParser.extractMerchant("Paid ₹250 to swiggy@icici"))
        assertEquals("zomato@hdfcbank", TransactionParser.extractMerchant("Payment of ₹450 to zomato@hdfcbank was successful"))
        assertEquals("uber.pay@axis", TransactionParser.extractMerchant("₹180 sent to uber.pay@axis via UPI"))
    }

    @Test
    fun `parse - falls back to title when merchant is in title`() {
        val raw = RawNotificationData(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Swiggy",
            text = "Paid ₹450 using UPI",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "key_swiggy"
        )
        val parsed = TransactionParser.parse(raw, TransactionDirection.DEBIT)
        assertNotNull(parsed)
        assertEquals(BigDecimal("450.00"), parsed?.amount)
        assertEquals("Swiggy", parsed?.merchant)
    }

    @Test
    fun `parse - does not treat bank or system alert title as merchant`() {
        val raw = RawNotificationData(
            packageName = "com.hdfc.banking",
            title = "HDFC Bank Alert",
            text = "₹1,200 debited for Amazon",
            subText = null,
            bigText = null,
            receivedAt = Instant.now(),
            notificationKey = "key_hdfc"
        )
        val parsed = TransactionParser.parse(raw, TransactionDirection.DEBIT)
        assertNotNull(parsed)
        assertEquals(BigDecimal("1200.00"), parsed?.amount)
        assertEquals("Amazon", parsed?.merchant)
    }
}

