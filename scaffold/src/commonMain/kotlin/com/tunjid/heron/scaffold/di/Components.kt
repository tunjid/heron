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


import com.tunjid.heron.data.di.DataComponent
import com.tunjid.heron.media.video.VideoPlayerController
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.NavigationStateHolder
import com.tunjid.heron.scaffold.navigation.PersistedNavigationStateHolder
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.strings.RouteMatcher
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.routeParserFrom
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.flow.StateFlow

abstract class ScaffoldScope private constructor()

class ScaffoldModule(
    val routeMatchers: List<RouteMatcher>,
    val videoPlayerController: VideoPlayerController,
)

@BindingContainer
class ScaffoldComponent(
    val module: ScaffoldModule,
    @Includes val dataComponent: DataComponent,
) {

    @Provides
    fun navStateStream(
        navStateHolder: NavigationStateHolder,
    ): StateFlow<MultiStackNav> = navStateHolder.state

    @Provides
    fun routeParser(): RouteParser =
        routeParserFrom(*(module.routeMatchers).toTypedArray())

    @Provides
    fun videoPlayerController(): VideoPlayerController =
        module.videoPlayerController

    @Provides
    fun navActions(
        navStateHolder: NavigationStateHolder,
    ): (NavigationMutation) -> Unit = navStateHolder.accept

    @Provides
    fun provideNavigationStateHolder(
        persistedNavigationStateHolder: PersistedNavigationStateHolder
    ): NavigationStateHolder = persistedNavigationStateHolder
}