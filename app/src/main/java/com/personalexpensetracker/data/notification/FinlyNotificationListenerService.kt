package com.personalexpensetracker.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.preferences.UserPreferencesImpl
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Android [NotificationListenerService] that receives posted notifications
 * and feeds them into the Finly automatic expense capture pipeline.
 *
 * This service requires the user to explicitly grant Notification Access
 * permission through Android Settings. It does NOT silently request or
 * bypass this permission.
 *
 * ## Lifecycle
 * - [onListenerConnected]: Called when the service is connected and permission is active.
 * - [onListenerDisconnected]: Called when the service is disconnected.
 * - [onNotificationPosted]: Called for each new notification; extracts data and
 *   passes it to the processing pipeline.
 * - [onNotificationRemoved]: Acknowledged but not acted upon.
 *
 * ## Privacy
 * - Full notification text is NOT logged in production.
 * - Only sanitized [RawNotificationData] is passed into the processing pipeline.
 * - No notification data is sent to external services.
 *
 * ## Background Behavior (Step 9)
 * - Uses notification events as triggers — no polling or continuous background loop.
 * - Service lifecycle is managed by Android's NotificationListenerService framework.
 * - SupervisorJob ensures one notification failure doesn't crash others.
 * - Handles service connect/disconnect/destroy gracefully.
 *
 * ## Auto-Capture Setting (Step 10)
 * - Respects the user's "Auto-Capture Expenses" preference.
 * - When disabled, notifications are received but not processed.
 * - Manual expense entry continues to work regardless.
 */
class FinlyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "FinlyNotifListener"

        /**
         * Track whether the listener is currently connected.
         * Useful for UI status display.
         */
        @Volatile
        var isListenerConnected: Boolean = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lazily initialized — constructed on first notification to avoid
    // heavy initialization in onListenerConnected
    private var processor: NotificationProcessor? = null
    private var userPreferences: UserPreferencesImpl? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        isListenerConnected = true
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListenerConnected = false
        Log.d(TAG, "Notification listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Check auto-capture preference
        val prefs = getOrCreatePreferences()
        val prefsData = prefs.getPreferences()
        if (!prefsData.autoCaptureExpenses) {
            return // Auto-capture is disabled — skip processing
        }

        val notificationData = extractNotificationData(sbn) ?: return

        serviceScope.launch {
            try {
                val pipeline = getOrCreateProcessor()
                val result = pipeline.process(notificationData)
                handleResult(result)
            } catch (e: Exception) {
                // Absolute safety net — never crash the listener service
                Log.e(TAG, "Unexpected error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Acknowledged — no action required.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        isListenerConnected = false
        processor = null
        userPreferences = null
        Log.d(TAG, "Notification listener service destroyed")
    }

    /**
     * Lazily creates the full pipeline processor using the shared pipeline.
     */
    private fun getOrCreateProcessor(): NotificationProcessor {
        return processor ?: AutoExpenseCapturePipeline.getProcessor(applicationContext).also {
            processor = it
        }
    }

    /**
     * Lazily creates the user preferences accessor.
     */
    private fun getOrCreatePreferences(): UserPreferencesImpl {
        return userPreferences ?: UserPreferencesImpl(applicationContext).also {
            userPreferences = it
        }
    }

    /**
     * Extracts sanitized [RawNotificationData] from a [StatusBarNotification].
     * Returns null if the notification cannot be read safely. Never throws.
     */
    private fun extractNotificationData(sbn: StatusBarNotification): RawNotificationData? {
        return try {
            val notification = sbn.notification ?: return null
            val extras = notification.extras

            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

            // Extract InboxStyle text lines if available
            val textLines = mutableListOf<String>()
            val linesArray = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (linesArray != null) {
                for (line in linesArray) {
                    val lineStr = line?.toString()?.trim()
                    if (!lineStr.isNullOrBlank()) {
                        textLines.add(lineStr)
                    }
                }
            }

            // Extract MessagingStyle messages if available
            val messages = mutableListOf<String>()
            if (extras != null) {
                val messagesBundleArray = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (messagesBundleArray != null) {
                    for (msgObj in messagesBundleArray) {
                        if (msgObj is android.os.Bundle) {
                            val msgText = msgObj.getCharSequence("text")?.toString()?.trim()
                            if (!msgText.isNullOrBlank()) {
                                messages.add(msgText)
                            }
                        }
                    }
                }
            }

            RawNotificationData(
                packageName = sbn.packageName ?: "",
                title = title,
                text = text,
                subText = subText,
                bigText = bigText,
                receivedAt = Instant.now(),
                notificationKey = sbn.key,
                textLines = textLines,
                messages = messages
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract notification data", e)
            null
        }
    }

    /**
     * Handles the processing result with minimal, privacy-conscious logging.
     * No full notification text is logged.
     */
    private fun handleResult(result: NotificationProcessingResult) {
        when (result) {
            is NotificationProcessingResult.ExpenseCreated -> {
                Log.d(TAG, "Expense auto-created: id=${result.expenseId}, " +
                    "amount=${result.parsedTransaction.amount}, " +
                    "category=${result.category}")
            }
            is NotificationProcessingResult.IncomeCreated -> {
                Log.d(TAG, "Income auto-created: id=${result.incomeId}, " +
                    "amount=${result.parsedTransaction.amount}, " +
                    "source=${result.source}")
            }
            is NotificationProcessingResult.Received -> {
                Log.d(TAG, "Notification received from: ${result.data.packageName}")
            }
            is NotificationProcessingResult.Ignored -> {
                // Minimal logging for ignored notifications
            }
            is NotificationProcessingResult.CreditIgnored -> {
                Log.d(TAG, "Credit notification ignored")
            }
            is NotificationProcessingResult.Ambiguous -> {
                Log.d(TAG, "Ambiguous notification ignored")
            }
            is NotificationProcessingResult.InvalidTransaction -> {
                Log.d(TAG, "Invalid transaction: ${result.reason}")
            }
            is NotificationProcessingResult.Duplicate -> {
                Log.d(TAG, "Duplicate notification ignored")
            }
            is NotificationProcessingResult.PendingProcessing -> {
                Log.d(TAG, "Pending processing")
            }
            is NotificationProcessingResult.Error -> {
                Log.e(TAG, "Processing error: ${result.message}")
            }
        }
    }
}
