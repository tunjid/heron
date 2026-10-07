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

package com.tunjid.heron.ui.scaffold.di

import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.tasks.BackgroundTaskDescriptor
import com.tunjid.heron.data.tasks.BackgroundTaskRunner
import com.tunjid.heron.data.tasks.BackgroundTaskScheduler
import com.tunjid.heron.media.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.identity.AppIdentityStateHolder
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.AppNotificationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.Notifier
import com.tunjid.heron.ui.scaffold.scaffold.AppState
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PredictiveBackContentTransformer
import com.tunjid.heron.ui.scaffold.tasks.AppBackgroundTaskDescriptor
import com.tunjid.heron.ui.scaffold.ui.AppUiStateHolder
import com.tunjid.heron.ui.scaffold.ui.UiStateHolder
import com.tunjid.heron.ui.stateproduction.RouteStateHolderInitializer
import com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer
import com.tunjid.treenav.compose.PaneEntry
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

class ScaffoldBindingArgs(
    val imageLoader: ImageLoader,
    val notifier: Notifier,
    val videoPlayerController: VideoPlayerController,
    val routeMatchers: List<RouteMatcher>,
)

@BindingContainer
@ContributesTo(AppScope::class)
object ScaffoldBindings {

    @SingleIn(AppScope::class)
    @Provides
    fun routeParser(
        args: ScaffoldBindingArgs,
    ): RouteParser =
        routeParserFrom(*(args.routeMatchers).toTypedArray())

    @SingleIn(AppScope::class)
    @Provides
    fun imageLoader(
        args: ScaffoldBindingArgs,
    ): ImageLoader =
        args.imageLoader

    @SingleIn(AppScope::class)
    @Provides
    fun notifier(
        args: ScaffoldBindingArgs,
    ): Notifier =
        args.notifier

    @SingleIn(AppScope::class)
    @Provides
    fun videoPlayerController(
        args: ScaffoldBindingArgs,
    ): VideoPlayerController =
        args.videoPlayerController

    @SingleIn(AppScope::class)
    @Provides
    fun backgroundTaskDescriptor(
        descriptor: AppBackgroundTaskDescriptor,
    ): BackgroundTaskDescriptor =
        descriptor

    @Provides
    fun provideNavigationContentTransformer(): NavigationContentTransformer =
        PredictiveBackContentTransformer

    @SingleIn(AppScope::class)
    @Provides
    internal fun navActions(
        navStateHolder: NavigationStateHolder,
    ): (NavigationMutation) -> Unit = navStateHolder.accept

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder,
    ): NavigationStateHolder = persistedNavigationStateHolder

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideNotificationStateHolder(
        appNotificationStateHolder: AppNotificationStateHolder,
    ): NotificationStateHolder = appNotificationStateHolder

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideIdentityStateHolder(
        appIdentityStateHolder: AppIdentityStateHolder,
    ): IdentityStateHolder = appIdentityStateHolder

    @SingleIn(AppScope::class)
    @Provides
    internal fun provideUiStateHolder(
        appUiStateHolder: AppUiStateHolder,
    ): UiStateHolder = appUiStateHolder

    @SingleIn(AppScope::class)
    @Provides
    internal fun appState(
        @AppMainScope
        appMainScope: CoroutineScope,
        entryMap: Map<String, PaneEntry<ThreePane, Route>>,
        identityStateHolder: IdentityStateHolder,
        navigationStateHolder: NavigationStateHolder,
        notificationStateHolder: NotificationStateHolder,
        uiStateHolder: UiStateHolder,
        imageLoader: ImageLoader,
        videoPlayerController: VideoPlayerController,
        sheetStateHolderInitializers: Map<KClass<*>, SheetStateHolderInitializer>,
        routeStateHolderInitializers: Map<KClass<*>, RouteStateHolderInitializer>,
        backgroundTaskScheduler: BackgroundTaskScheduler,
        backgroundTaskRunner: BackgroundTaskRunner,
        backgroundTaskDescriptor: BackgroundTaskDescriptor,
    ): AppState = AppState(
        entryMap = entryMap,
        identityStateHolder = identityStateHolder,
        navigationStateHolder = navigationStateHolder,
        notificationStateHolder = notificationStateHolder,
        uiStateHolder = uiStateHolder,
        imageLoader = imageLoader,
        videoPlayerController = videoPlayerController,
        sheetStateHolderInitializers = sheetStateHolderInitializers,
        routeStateHolderInitializers = routeStateHolderInitializers,
        backgroundTaskScheduler = backgroundTaskScheduler,
        backgroundTaskRunner = backgroundTaskRunner,
        backgroundTaskDescriptor = backgroundTaskDescriptor,
        processScope = appMainScope,
    )
}
