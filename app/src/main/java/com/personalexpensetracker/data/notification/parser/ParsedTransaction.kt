package com.personalexpensetracker.data.notification.parser

import com.personalexpensetracker.data.notification.detection.TransactionDirectionClassifier.TransactionDirection
import java.math.BigDecimal
import java.time.Instant

/**
 * Structured representation of a parsed financial transaction extracted
 * from a notification.
 *
 * This model carries only the information needed for expense creation
 * and duplicate detection. It does NOT store the raw notification text
 * permanently.
 *
 * @property amount The transaction amount in the user's currency.
 * @property merchant The merchant/payee name, or null if not extractable.
 * @property direction The classified transaction direction (DEBIT/CREDIT/UNKNOWN).
 * @property transactionTime The time of the transaction (notification received time).
 * @property sourcePackage The originating app package name.
 * @property referenceId Transaction/reference ID if available from the notification.
 * @property notificationKey Android notification key for deduplication.
 * @property accountLast4 Trailing 3-4 digits of the account/card if identified.
 */
data class ParsedTransaction(
    val amount: BigDecimal,
    val merchant: String?,
    val direction: TransactionDirection,
    val transactionTime: Instant,
    val sourcePackage: String,
    val referenceId: String? = null,
    val notificationKey: String? = null,
    val accountLast4: String? = null
)
