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

package com.tunjid.heron.ui.scaffold.scaffold

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation3.runtime.NavEntryDecorator
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.tasks.BackgroundTaskDescriptor
import com.tunjid.heron.data.tasks.BackgroundTaskHost
import com.tunjid.heron.data.tasks.BackgroundTaskRunner
import com.tunjid.heron.data.tasks.BackgroundTaskScheduler
import com.tunjid.heron.media.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.navigation.deepLinkTo
import com.tunjid.heron.ui.scaffold.navigation.isShowingSplashScreen
import com.tunjid.heron.ui.scaffold.notifications.NotificationAction
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.scaffold.AppState.Companion.NOTIFICATION_PROCESSING_TIMEOUT_SECONDS
import com.tunjid.heron.ui.scaffold.ui.UiStateHolder
import com.tunjid.heron.ui.scaffold.ui.isImmersive
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.ViewModelBackedStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.withSnapshotNotifications
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.strings.PathPattern
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.toRouteTrie
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/**
 * Application level state.
 */
@Stable
class AppState internal constructor(
    entryMap: Map<String, PaneEntry<ThreePane, Route>>,
    private val identityStateHolder: IdentityStateHolder,
    private val navigationStateHolder: NavigationStateHolder,
    private val notificationStateHolder: NotificationStateHolder,
    private val uiStateHolder: UiStateHolder,
    internal val imageLoader: ImageLoader,
    internal val videoPlayerController: VideoPlayerController,
    internal val sheetStateHolderInitializers: Map<KClass<*>, SheetStateHolderInitializer>,
    internal val routeStateHolderInitializers: Map<KClass<*>, RouteStateHolderInitializer>,
    override val backgroundTaskScheduler: BackgroundTaskScheduler,
    override val backgroundTaskRunner: BackgroundTaskRunner,
    override val backgroundTaskDescriptor: BackgroundTaskDescriptor,
    override val processScope: CoroutineScope,
) : BackgroundTaskHost {
    var showPlatformSplashScreen by mutableStateOf(true)
        private set

    internal val splashVisibilityNavEntryDecorator: NavEntryDecorator<Route> =
        NavEntryDecorator { entry ->
            entry.Content()
            if (showPlatformSplashScreen) {
                LifecycleStartEffect(Unit) {
                    showPlatformSplashScreen = false
                    onStopOrDispose { }
                }
            }
        }

    internal val stateHolderInitializer = ViewModelBackedStateHolderInitializer(
        routeStateHolderInitializers = routeStateHolderInitializers,
        sheetStateHolderInitializers = sheetStateHolderInitializers,
    )

    private val entryTrie = entryMap
        .mapKeys { (template) -> PathPattern(template) }
        .toRouteTrie()

    internal fun entry(route: Route) =
        entryTrie[route] ?: threePaneEntry(
            render = {
                NotFoundRoute(
                    onGoBack = {
                        navigationStateHolder.accept(NavigationAction.Pop.navigationMutation)
                    },
                    onGoHome = {
                        navigationStateHolder.accept(NavigationAction.Home.navigationMutation)
                    },
                )
            },
        )

    fun onDeepLink(uri: GenericUri) =
        navigationStateHolder.accept(deepLinkTo(uri))

    fun onPushTokenRegistered(
        token: String,
    ) = notificationStateHolder.accept(
        NotificationAction.RegisterToken(
            token = token,
        ),
    )

    fun onNotificationPermissionsChanged(
        hasNotificationPermissions: Boolean,
    ) = notificationStateHolder.accept(
        NotificationAction.UpdatePermissions(
            hasNotificationPermissions = hasNotificationPermissions,
        ),
    )

    fun onNotificationDismissed(
        dismissedAt: Instant,
    ) = notificationStateHolder.accept(
        NotificationAction.NotificationDismissed(
            dismissedAt = dismissedAt,
        ),
    )

    /**
     * Processes a push notification [payload], suspending until it has been processed
     * or [NOTIFICATION_PROCESSING_TIMEOUT_SECONDS] elapses, to keep the platform from
     * killing the app due to background execution limits.
     *
     * This method is called from outside compose and needs manual snapshot observation.
     */
    suspend fun processPushNotification(
        payload: Map<String, String>,
    ) {
        val action = NotificationAction.HandleNotification(payload = payload)
        action.senderDid ?: return
        val recordUri = action.recordUri ?: return

        logcat(LogPriority.DEBUG) {
            "Received notification for $recordUri. Payload: $payload"
        }
        notificationStateHolder.accept(action)

        try {
            withTimeout(NOTIFICATION_PROCESSING_TIMEOUT_SECONDS) {
                withSnapshotNotifications {
                    snapshotFlow {
                        notificationStateHolder.state.processedNotificationRecordUris
                    }.first {
                        recordUri in it
                    }
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) {
                "Notification processing timed out or failed for $recordUri. Cause: ${e.loggableText()}"
            }
        } finally {
            notificationStateHolder.accept(
                NotificationAction.NotificationProcessedOrDropped(
                    recordUri = recordUri,
                ),
            )
        }
    }

    companion object {
        internal val NOTIFICATION_PROCESSING_TIMEOUT_SECONDS = 10.seconds

        val AppState.isShowingSplashScreen: Boolean
            get() = navigationStateHolder.state.multiStackNav.isShowingSplashScreen

        val AppState.isImmersive: Boolean
            get() = uiStateHolder.state.isImmersive

        internal fun AppState.staticStates() = AppScaffoldState.StaticStates(
            identityStateHolder = identityStateHolder,
            navigationStateHolder = navigationStateHolder,
            notificationStateHolder = notificationStateHolder,
            uiStateHolder = uiStateHolder,
            imageLoader = imageLoader,
            videoPlayerController = videoPlayerController,
            stateHolderInitializer = stateHolderInitializer,
        )
    }
}

internal val LocalAppScaffoldState = staticCompositionLocalOf<AppScaffoldState> {
    throw IllegalStateException("No AppScaffoldState provided")
}
