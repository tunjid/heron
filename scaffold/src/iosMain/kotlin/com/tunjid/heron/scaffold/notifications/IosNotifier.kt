/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.scaffold.notifications

import com.tunjid.heron.data.core.models.Notification
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

class IosNotifier : Notifier {

    override suspend fun displayNotifications(notifications: List<Notification>) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val showingIds = getDeliveredNotificationIds(center)

        for (notification in notifications) {
            val notificationId = notification.uri.uri
            if (notificationId in showingIds) continue

            val content = UNMutableNotificationContent().apply {
                setTitle(notification.title())
                notification.body()?.let(::setBody)
                setUserInfo(
                    mapOf<Any?, Any>(
                        DEEP_LINK_SCHEME_KEY to notification.deepLinkScheme(),
                        DEEP_LINK_PATH_KEY to notification.deepLinkPath(),
                    ),
                )
            }

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = notificationId,
                content = content,
                trigger = null,
            )

            center.addNotificationRequest(request, withCompletionHandler = null)
        }
    }

    companion object {
        const val DEEP_LINK_SCHEME_KEY = "deepLinkScheme"
        const val DEEP_LINK_PATH_KEY = "deepLinkPath"
    }
}

private suspend fun getDeliveredNotificationIds(
    center: UNUserNotificationCenter,
): Set<String> = suspendCoroutine { continuation ->
    center.getDeliveredNotificationsWithCompletionHandler { delivered ->
        val ids = delivered
            ?.mapNotNullTo(mutableSetOf()) {
                (it as? platform.UserNotifications.UNNotification)
                    ?.request
                    ?.identifier
            }
            ?: emptySet()
        continuation.resume(ids)
    }
}
