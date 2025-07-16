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

package com.tunjid.heron.scaffold.di


import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.StateFlow

class ScaffoldBindingArgs(
    val routeMatchers: List<RouteMatcher>,
    val videoPlayerController: VideoPlayerController,
)

@BindingContainer
class ScaffoldBindings(
    val args: ScaffoldBindingArgs,
    @Includes val dataBindings: DataBindings,
) {

    @SingleIn(AppScope::class)
    @Provides
    fun navStateStream(
        navStateHolder: NavigationStateHolder,
    ): StateFlow<MultiStackNav> = navStateHolder.state

    @SingleIn(AppScope::class)
    @Provides
    fun routeParser(): RouteParser =
        routeParserFrom(*(args.routeMatchers).toTypedArray())

    @SingleIn(AppScope::class)
    @Provides
    fun videoPlayerController(): VideoPlayerController =
        args.videoPlayerController

    @SingleIn(AppScope::class)
    @Provides
    fun navActions(
        navStateHolder: NavigationStateHolder,
    ): (NavigationMutation) -> Unit = navStateHolder.accept

    @SingleIn(AppScope::class)
    @Provides
    fun provideNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder
    ): NavigationStateHolder = persistedNavigationStateHolder
}