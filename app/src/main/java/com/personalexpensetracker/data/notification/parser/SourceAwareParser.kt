package com.personalexpensetracker.data.notification.parser

/**
 * Source-aware notification handling — identifies the origin of financial
 * notifications and provides parsing context.
 *
 * ## Design
 * - Not a giant hardcoded rule set. Instead, categorizes known financial
 *   app packages and provides confidence signals to the parser.
 * - Unknown sources are still processed — source is a hint, not a gate.
 * - Extensible: new sources can be added to the appropriate category.
 */
object SourceAwareParser {

    /**
     * The type of financial source application.
     */
    enum class SourceType {
        /** Banking applications (SBI, HDFC, ICICI, etc.) */
        BANK,
        /** UPI payment apps (GPay, PhonePe, Paytm, etc.) */
        UPI,
        /** Credit/Debit card apps */
        CARD,
        /** Digital wallet apps */
        WALLET,
        /** General payment/fintech apps */
        PAYMENT,
        /** SMS (bank sends transaction SMS) */
        SMS,
        /** Unknown or non-financial source */
        UNKNOWN
    }

    /**
     * Known financial app packages mapped to their source type.
     * This is NOT an exhaustive list — unknown packages are still processed.
     */
    private val KNOWN_PACKAGES = mapOf(
        // Banks
        "com.sbi.lotusintouch" to SourceType.BANK,          // SBI YONO
        "com.sbi.SBIFreedomPlus" to SourceType.BANK,        // SBI Freedom
        "com.csam.icici.bank.imobile" to SourceType.BANK,    // ICICI iMobile
        "com.icicibank.sa" to SourceType.BANK,               // ICICI
        "com.hdfc.retail.banking" to SourceType.BANK,        // HDFC
        "com.axis.mobile" to SourceType.BANK,                // Axis
        "com.msf.koenig.kotak" to SourceType.BANK,           // Kotak
        "com.bob.banking" to SourceType.BANK,                // BOB
        "com.pnb.mbanking" to SourceType.BANK,               // PNB
        "com.unionbank" to SourceType.BANK,                   // Union Bank
        "com.canarabank.mobility" to SourceType.BANK,        // Canara Bank

        // UPI
        "com.google.android.apps.nbu.paisa.user" to SourceType.UPI, // GPay
        "net.one97.paytm" to SourceType.UPI,                 // Paytm
        "com.phonepe.app" to SourceType.UPI,                 // PhonePe
        "in.amazon.mShop.android.shopping" to SourceType.UPI, // Amazon Pay
        "com.whatsapp" to SourceType.UPI,                    // WhatsApp Pay
        "in.org.npci.upiapp" to SourceType.UPI,              // BHIM

        // Cards
        "com.cred.android" to SourceType.CARD,               // CRED
        "com.dreamplug.androidapp" to SourceType.CARD,       // CRED alt

        // Wallets
        "com.mobikwik_new" to SourceType.WALLET,             // MobiKwik
        "com.freecharge.android" to SourceType.WALLET,       // FreeCharge

        // SMS (Android Messages / OEM default SMS apps / Truecaller)
        "com.google.android.apps.messaging" to SourceType.SMS,
        "com.android.mms" to SourceType.SMS,
        "com.samsung.android.messaging" to SourceType.SMS,
        "com.miui.mms" to SourceType.SMS,
        "com.coloros.mms" to SourceType.SMS,
        "com.oppo.mms" to SourceType.SMS,
        "com.oneplus.mms" to SourceType.SMS,
        "com.vivo.mms" to SourceType.SMS,
        "com.motorola.messaging" to SourceType.SMS,
        "com.truecaller" to SourceType.SMS,
        "sms" to SourceType.SMS
    )

    /**
     * Package name substrings that suggest a financial source even when
     * the exact package is not in the known list.
     */
    private val FINANCIAL_PACKAGE_HINTS = listOf(
        "bank", "banking", "finance", "fintech",
        "pay", "payment", "upi", "wallet",
        "money", "credit", "debit"
    )

    /**
     * Identifies the source type for a given package name.
     *
     * @param packageName The notification's originating package.
     * @return The identified [SourceType].
     */
    fun identifySource(packageName: String): SourceType {
        if (packageName.isBlank()) return SourceType.UNKNOWN

        // Check exact match first
        KNOWN_PACKAGES[packageName]?.let { return it }

        // Check SMS / messaging apps pattern
        val lowerPackage = packageName.lowercase()
        if (lowerPackage == "sms" ||
            lowerPackage.contains(".mms") ||
            lowerPackage.contains(".messaging") ||
            lowerPackage.endsWith(".sms") ||
            lowerPackage.contains("telephony")
        ) {
            return SourceType.SMS
        }

        // Check package name hints
        if (FINANCIAL_PACKAGE_HINTS.any { lowerPackage.contains(it) }) {
            return SourceType.PAYMENT // Generic financial
        }

        return SourceType.UNKNOWN
    }

    /**
     * Returns true if the package represents an SMS / default messaging application.
     */
    fun isMessagingApp(packageName: String): Boolean {
        return identifySource(packageName) == SourceType.SMS
    }

    /**
     * Returns whether a given source is likely to produce financial
     * transaction notifications.
     */
    fun isFinancialSource(packageName: String): Boolean {
        return identifySource(packageName) != SourceType.UNKNOWN
    }

    /**
     * Returns a confidence boost for financial detection based on the source.
     * Known financial sources get a boost, unknown sources don't.
     */
    fun getSourceConfidenceBoost(packageName: String): Float {
        return when (identifySource(packageName)) {
            SourceType.BANK -> 0.9f
            SourceType.UPI -> 0.8f
            SourceType.CARD -> 0.8f
            SourceType.WALLET -> 0.7f
            SourceType.PAYMENT -> 0.6f
            SourceType.SMS -> 0.5f
            SourceType.UNKNOWN -> 0.0f
        }
    }
}
