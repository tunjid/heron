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

import com.tunjid.heron.images.ImageLoader
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.ui.scaffold.identity.AppIdentityStateHolder
import com.tunjid.heron.ui.scaffold.identity.IdentityStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.ui.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.AppNotificationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.NotificationStateHolder
import com.tunjid.heron.ui.scaffold.notifications.Notifier
import com.tunjid.heron.ui.scaffold.scaffold.NavigationContentTransformer
import com.tunjid.heron.ui.scaffold.scaffold.PredictiveBackContentTransformer
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.StateFlow

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

    @Provides
    fun provideNavigationContentTransformer(): NavigationContentTransformer =
        PredictiveBackContentTransformer

    @SingleIn(AppScope::class)
    @Provides
    fun navActions(
        navStateHolder: NavigationStateHolder,
    ): (NavigationMutation) -> Unit = navStateHolder.accept

    @SingleIn(AppScope::class)
    @Provides
    fun provideNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder,
    ): NavigationStateHolder = persistedNavigationStateHolder

    @SingleIn(AppScope::class)
    @Provides
    fun provideNotificationStateHolder(
        appNotificationStateHolder: AppNotificationStateHolder,
    ): NotificationStateHolder = appNotificationStateHolder

    @SingleIn(AppScope::class)
    @Provides
    fun provideIdentityStateHolder(
        appIdentityStateHolder: AppIdentityStateHolder,
    ): IdentityStateHolder = appIdentityStateHolder
}
