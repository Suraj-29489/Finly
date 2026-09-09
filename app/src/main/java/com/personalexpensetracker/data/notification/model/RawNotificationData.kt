package com.personalexpensetracker.data.notification.model

import java.time.Instant

/**
 * Immutable, sanitized representation of an incoming Android notification
 * that has been received by the NotificationListenerService.
 *
 * This is the first stage of the automatic expense capture pipeline.
 * It carries only the information needed for financial transaction detection
 * and parsing — no raw StatusBarNotification or Android framework objects leak
 * beyond this boundary.
 *
 * @property packageName The originating application's package name (e.g. "com.sbi.lotusintouch").
 * @property title The notification title text, if available.
 * @property text The notification body/content text, if available.
 * @property subText Additional sub-text from the notification, if available.
 * @property bigText Expanded "big text" style content, if available.
 * @property receivedAt The instant the notification was received by Finly.
 * @property notificationKey Android notification key for deduplication reference.
 */
data class RawNotificationData(
    val packageName: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val receivedAt: Instant,
    val notificationKey: String?,
    val textLines: List<String> = emptyList(),
    val messages: List<String> = emptyList()
) {
    /**
     * Returns the most complete body text representation available for this notification.
     * Prefers bigText (which contains the full expanded message) over the truncated text field,
     * or textLines/messages if available. Does not include title.
     */
    val fullText: String?
        get() = bigText?.takeIf { it.isNotBlank() }
            ?: text?.takeIf { it.isNotBlank() }
            ?: textLines.firstOrNull { it.isNotBlank() }
            ?: messages.firstOrNull { it.isNotBlank() }

    /**
     * Returns all available text content concatenated for comprehensive analysis.
     * Combines title, text, subText, bigText, textLines, and messages.
     */
    val combinedText: String
        get() = (listOfNotNull(title, text, subText, bigText) + textLines + messages)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")

    /**
     * Whether this notification contains any textual content at all.
     */
    val hasContent: Boolean
        get() = !title.isNullOrBlank() || !text.isNullOrBlank() ||
                !subText.isNullOrBlank() || !bigText.isNullOrBlank() ||
                textLines.any { it.isNotBlank() } || messages.any { it.isNotBlank() }
}

