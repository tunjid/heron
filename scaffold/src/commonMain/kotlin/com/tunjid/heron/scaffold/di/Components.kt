/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class ScaffoldScope

class ScaffoldModule(
    val routeMatchers: List<RouteMatcher>,
    val videoPlayerController: VideoPlayerController,
)

@KmpComponentCreate
expect fun ScaffoldComponent.Companion.create(
    module: ScaffoldModule,
    dataComponent: DataComponent,
): ScaffoldComponent

@ScaffoldScope
@Component
abstract class ScaffoldComponent(
    private val module: ScaffoldModule,
    @Component val dataComponent: DataComponent,
) {

    @Provides
    fun navStateStream(
        navStateHolder: NavigationStateHolder,
    ): StateFlow<MultiStackNav> = navStateHolder.state

    @Provides
    fun routeParser(): RouteParser =
        routeParserFrom(*(module.routeMatchers).toTypedArray())

    @Provides
    fun navActions(): (NavigationMutation) -> Unit = navStateHolder.accept

    val PersistedNavigationStateHolder.bind: NavigationStateHolder
        @ScaffoldScope
        @Provides get() = this

    abstract val navStateHolder: NavigationStateHolder

    companion object
}