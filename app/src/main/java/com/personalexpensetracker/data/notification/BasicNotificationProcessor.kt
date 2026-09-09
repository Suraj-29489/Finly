package com.personalexpensetracker.data.notification

import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData

/**
 * Step 1 implementation of [NotificationProcessor].
 *
 * At this stage the processor simply acknowledges receipt of notifications
 * that contain content, and ignores empty/contentless ones.
 *
 * In later steps, this will be replaced by the full pipeline that chains:
 * financial detection → debit/credit classification → parsing →
 * category resolution → duplicate detection → expense creation.
 */
class BasicNotificationProcessor : NotificationProcessor {

    override suspend fun process(notification: RawNotificationData): NotificationProcessingResult {
        return try {
            if (!notification.hasContent) {
                NotificationProcessingResult.Ignored("Notification has no text content")
            } else {
                // Step 1: Simply acknowledge receipt and pass into pipeline
                NotificationProcessingResult.Received(notification)
            }
        } catch (e: Exception) {
            // Never crash — capture the error
            NotificationProcessingResult.Error(
                message = "Processing failed: ${e.message}",
                data = notification
            )
        }
    }
}

