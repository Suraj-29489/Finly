package com.personalexpensetracker.data.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.personalexpensetracker.data.notification.AutoExpenseCapturePipeline
import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Local-only utility that scans recent SMS messages from Android's Telephony ContentProvider
 * to automatically capture expenses.
 */
class SmsReader(private val context: Context) {

    companion object {
        private const val TAG = "SmsReader"
        private const val MAX_SMS_TO_SCAN = 50
    }

    /**
     * Scans recent inbox SMS messages from the past [lookbackHours] hours.
     * Returns the count of successfully created expenses.
     */
    suspend fun syncRecentSms(lookbackHours: Long = 48): Int = withContext(Dispatchers.IO) {
        var createdCount = 0
        val contentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val cutoffMillis = System.currentTimeMillis() - (lookbackHours * 3600 * 1000)
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffMillis.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)

                val processor = AutoExpenseCapturePipeline.getProcessor(context)
                var count = 0

                while (cursor.moveToNext() && count < MAX_SMS_TO_SCAN) {
                    count++
                    val id = cursor.getLong(idIdx)
                    val sender = cursor.getString(addressIdx) ?: "SMS"
                    val body = cursor.getString(bodyIdx) ?: continue
                    val dateMillis = cursor.getLong(dateIdx)
                    val timestamp = Instant.ofEpochMilli(dateMillis)

                    val rawData = RawNotificationData(
                        packageName = "sms",
                        title = sender,
                        text = body,
                        subText = "SMS Inbox",
                        bigText = body,
                        receivedAt = timestamp,
                        notificationKey = "sms_inbox_$id"
                    )

                    val result = processor.process(rawData)
                    if (result is NotificationProcessingResult.ExpenseCreated || result is NotificationProcessingResult.IncomeCreated) {
                        createdCount++
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SMS read permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS inbox", e)
        }

        createdCount
    }
}
