package com.personalexpensetracker.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.personalexpensetracker.data.notification.AutoExpenseCapturePipeline
import com.personalexpensetracker.data.notification.model.RawNotificationData
import com.personalexpensetracker.data.preferences.UserPreferencesImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Broadcast receiver that intercepts incoming financial SMS messages
 * and forwards them to the automatic expense capture pipeline.
 *
 * 100% Local-only: No SMS content is uploaded or transmitted outside the device.
 * Only active when the user has explicitly enabled SMS Auto-Capture in Settings.
 */
class FinlySmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "FinlySmsReceiver"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = UserPreferencesImpl(context)
        val prefsData = prefs.getPreferences()
        if (!prefsData.autoCaptureSms) {
            return // User has not enabled SMS Auto-Capture
        }

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract SMS from intent", e)
            return
        }

        if (messages.isNullOrEmpty()) return

        // Group multipart SMS messages by originating address (e.g. VK-HDFCBK, AD-SBIINB)
        val groupedBySender = messages.groupBy { it.originatingAddress ?: "SMS" }

        for ((sender, smsList) in groupedBySender) {
            val fullBody = smsList.joinToString(separator = "") { it.messageBody ?: "" }
            val timestamp = smsList.firstOrNull()?.timestampMillis?.let { Instant.ofEpochMilli(it) }
                ?: Instant.now()

            val rawData = RawNotificationData(
                packageName = "sms",
                title = sender,
                text = fullBody,
                subText = "SMS",
                bigText = fullBody,
                receivedAt = timestamp,
                notificationKey = "sms_${sender}_${timestamp.toEpochMilli()}"
            )

            val pendingResult = goAsync()
            receiverScope.launch {
                try {
                    val processor = AutoExpenseCapturePipeline.getProcessor(context)
                    val result = processor.process(rawData)
                    Log.d(TAG, "SMS capture result: $result")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS in pipeline", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
