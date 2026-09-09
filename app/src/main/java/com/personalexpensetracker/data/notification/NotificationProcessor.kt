package com.personalexpensetracker.data.notification

import com.personalexpensetracker.data.notification.model.NotificationProcessingResult
import com.personalexpensetracker.data.notification.model.RawNotificationData

/**
 * Pipeline interface for processing incoming notifications.
 *
 * The [NotificationProcessor] receives a [RawNotificationData] and returns a
 * [NotificationProcessingResult] describing the outcome. Each stage of the
 * automatic expense capture pipeline can be composed behind this interface.
 *
 * In Step 1, the processor simply acknowledges receipt. In later steps,
 * the implementation will chain financial detection → debit/credit classification
 * → parsing → category resolution → duplicate detection → expense creation.
 */
interface NotificationProcessor {

    /**
     * Process an incoming notification and return the result.
     *
     * This method must NEVER throw. Any errors must be captured and returned
     * as [NotificationProcessingResult.Error].
     *
     * @param notification The sanitized notification data.
     * @return The processing result.
     */
    suspend fun process(notification: RawNotificationData): NotificationProcessingResult
}

