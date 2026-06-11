package com.tunjid.heron.scaffold.notifications

import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.asRecordUriOrNull
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlin.time.Instant

@Snapshottable
interface NotificationState {
    @SnapshotSpec
    data class Immutable(
        val unreadCount: Long = 0L,
        val hasNotificationPermissions: Boolean = false,
        val notificationToken: String? = null,
        val processedNotificationRecordUris: Set<RecordUri> = emptySet(),
    ) : NotificationState
}

sealed class NotificationAction(
    val key: String,
) {
    data class UpdatePermissions(
        val hasNotificationPermissions: Boolean,
    ) : NotificationAction(key = "UpdatePermissions")

    data class RegisterToken(
        val token: String,
    ) : NotificationAction(key = "RegisterToken")

    data class HandleNotification(
        val payload: Map<String, String>,
    ) : NotificationAction(key = "HandleNotification") {
        val senderDid = payload[NotificationAtProtoSenderDid]
            ?.let(::ProfileId)

        val targetDid = payload[NotificationAtProtoTargetDid]
            ?.let(::ProfileId)

        val recordUri: RecordUri? = payload[NotificationAtProtoRecordUri]
            ?.asRecordUriOrNull()

        val reason: Notification.Reason? = payload[NotificationAtProtoReason]
            ?.let(Notification.Reason::fromIdOrNull)
    }

    data class NotificationProcessedOrDropped(
        val recordUri: RecordUri,
    ) : NotificationAction(key = "NotificationProcessedOrDropped")

    data class NotificationDismissed(
        val dismissedAt: Instant,
    ) : NotificationAction(key = "NotificationDismissed")

    data class ToggleUnreadNotificationsMonitor(
        val monitor: Boolean,
    ) : NotificationAction(key = "ToggleUnreadNotificationsMonitor")

    data object RequestedNotificationPermission :
        NotificationAction(key = "RequestedNotificationPermission")
}

private const val NotificationAtProtoSenderDid = "senderDid"
private const val NotificationAtProtoTargetDid = "targetDid"
private const val NotificationAtProtoRecordUri = "recordUri"
private const val NotificationAtProtoReason = "reason"
