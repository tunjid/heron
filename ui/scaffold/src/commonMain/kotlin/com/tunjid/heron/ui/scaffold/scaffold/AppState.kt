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
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.navigation.deepLinkTo
import com.tunjid.heron.ui.scaffold.navigation.isShowingSplashScreen
import com.tunjid.heron.ui.scaffold.notifications.NotificationAction
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
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
import kotlinx.coroutines.flow.first

@Stable
class AppState(
    entryMap: Map<String, PaneEntry<ThreePane, Route>>,
    private val identityStateHolder: IdentityStateHolder,
    private val navigationStateHolder: NavigationStateHolder,
    private val notificationStateHolder: NotificationStateHolder,
    internal val imageLoader: ImageLoader,
    internal val videoPlayerController: VideoPlayerController,
    internal val sheetStateHolderInitializers: Map<KClass<*>, SheetStateHolderInitializer>,
    internal val routeStateHolderInitializers: Map<KClass<*>, RouteStateHolderInitializer>,
) {
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
            render = { },
        )

    fun onDeepLink(uri: GenericUri) =
        navigationStateHolder.accept(deepLinkTo(uri))

    fun onNotificationAction(action: NotificationAction) =
        notificationStateHolder.accept(action)

    /**
     * This method is called from outside compose and
     * needs manual snapshot observation.
     */
    suspend fun awaitNotificationProcessing(
        recordUri: RecordUri,
    ) = withSnapshotNotifications {
        snapshotFlow {
            notificationStateHolder.state.processedNotificationRecordUris
        }.first {
            recordUri in it
        }
    }

    companion object {
        val NOTIFICATION_PROCESSING_TIMEOUT_SECONDS = 10.seconds

        val AppState.isShowingSplashScreen: Boolean
            get() = navigationStateHolder.state.multiStackNav.isShowingSplashScreen

        fun AppState.staticStates() = AppScaffoldState.StaticStates(
            identityStateHolder = identityStateHolder,
            navigationStateHolder = navigationStateHolder,
            notificationStateHolder = notificationStateHolder,
            imageLoader = imageLoader,
            videoPlayerController = videoPlayerController,
            stateHolderInitializer = stateHolderInitializer,
        )
    }
}

internal val LocalAppScaffoldState = staticCompositionLocalOf<AppScaffoldState> {
    throw IllegalStateException("No AppScaffoldState provided")
}
