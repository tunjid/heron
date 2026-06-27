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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
import com.tunjid.heron.ui.stateproduction.RouteViewModel
import com.tunjid.heron.ui.stateproduction.RouteViewModelInitializer
import com.tunjid.heron.ui.stateproduction.SheetViewModel
import com.tunjid.heron.ui.stateproduction.SheetViewModelInitializer
import com.tunjid.heron.ui.stateproduction.ViewModelInitializer
import com.tunjid.heron.ui.stateproduction.withSnapshotNotifications
import com.tunjid.mutator.compose.produceState
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.MultiPaneDisplayState
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.multiPaneDisplayBackstack
import com.tunjid.treenav.compose.panedecorators.PaneDecorator
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.compose.threepane.threePaneEntry
import com.tunjid.treenav.pop
import com.tunjid.treenav.requireCurrent
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
    internal val sheetViewModelInitializers: Map<KClass<*>, SheetViewModelInitializer>,
    internal val routeViewModelInitializers: Map<KClass<*>, RouteViewModelInitializer>,
) {
    var showPlatformSplashScreen by mutableStateOf(true)
        private set

    internal val viewModelInitializer = object : ViewModelInitializer {
        override fun sheetViewModelInitializer(
            modelClass: KClass<out SheetViewModel>,
        ): SheetViewModelInitializer =
            sheetViewModelInitializers[modelClass]
                ?: throw IllegalStateException(
                    "No SheetViewModelInitializer registered for ${modelClass.simpleName}. Ensure it is contributed in SheetBindings.",
                )

        override fun routeViewModelInitializer(
            modelClass: KClass<out RouteViewModel>,
        ): RouteViewModelInitializer =
            routeViewModelInitializers[modelClass]
                ?: throw IllegalStateException(
                    "No RouteViewModelInitializer registered for ${modelClass.simpleName}. Ensure it is contributed in the feature's Bindings.",
                )
    }

    private val entryTrie = entryMap
        .mapKeys { (template) -> PathPattern(template) }
        .toRouteTrie()

    private val splashVisibilityNavEntryDecorator: NavEntryDecorator<Route> =
        NavEntryDecorator { entry ->
            entry.Content()
            if (showPlatformSplashScreen) {
                LifecycleStartEffect(Unit) {
                    showPlatformSplashScreen = false
                    onStopOrDispose { }
                }
            }
        }

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

        @Composable
        internal fun AppState.rememberMultiPaneDisplayState(
            paneDecorators: List<PaneDecorator<MultiStackNav, Route, ThreePane>>,
        ): MultiPaneDisplayState<MultiStackNav, Route, ThreePane> {
            val saveableStateHolderNavEntryDecorator =
                rememberSaveableStateHolderNavEntryDecorator<Route>()
            val viewModelStoreNavEntryDecorator =
                rememberViewModelStoreNavEntryDecorator<Route>()

            val displayState = remember {
                MultiPaneDisplayState(
                    panes = ThreePane.entries.toList(),
                    paneDecorators = paneDecorators,
                    navigationState = derivedStateOf(navigationStateHolder.state::multiStackNav),
                    backStackTransform = MultiStackNav::multiPaneDisplayBackstack,
                    destinationTransform = MultiStackNav::requireCurrent,
                    popTransform = MultiStackNav::pop,
                    onPopped = { poppedNavigationState ->
                        navigationStateHolder.accept {
                            poppedNavigationState
                        }
                    },
                    navEntryDecorators = listOf(
                        saveableStateHolderNavEntryDecorator,
                        viewModelStoreNavEntryDecorator,
                        splashVisibilityNavEntryDecorator,
                    ),
                    entryProvider = { node ->
                        entryTrie[node] ?: threePaneEntry(
                            render = { },
                        )
                    },
                )
            }

            identityStateHolder.produceState()
            navigationStateHolder.produceState()
            notificationStateHolder.produceState()

            // TODO: Figure out a way to do this in the background with KMP
            LifecycleResumeEffect(Unit) {
                notificationStateHolder.accept(
                    NotificationAction.ToggleUnreadNotificationsMonitor(monitor = true),
                )
                onPauseOrDispose {
                    notificationStateHolder.accept(
                        NotificationAction.ToggleUnreadNotificationsMonitor(monitor = false),
                    )
                }
            }

            LifecycleStartEffect(videoPlayerController) {
                onStopOrDispose { videoPlayerController.pauseActiveVideo() }
            }

            return displayState
        }

        fun AppState.displayStates() = DisplayScaffoldState.StaticStates(
            identityStateHolder = identityStateHolder,
            navigationStateHolder = navigationStateHolder,
            notificationStateHolder = notificationStateHolder,
        )
    }
}

internal val LocalDisplayScaffoldState = staticCompositionLocalOf<DisplayScaffoldState> {
    throw IllegalStateException("No SplitPaneState provided")
}

internal val LocalAppState = staticCompositionLocalOf<AppState> {
    throw IllegalStateException("No AppState provided")
}
