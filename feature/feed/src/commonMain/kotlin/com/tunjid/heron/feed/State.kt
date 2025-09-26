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

package com.tunjid.heron.feed

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.scaffold.scaffold.ScaffoldMessage
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.treenav.strings.Route
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val creator: Profile? = null,
    val sharedElementPrefix: String? = null,
    @Transient
    val timelineState: TimelineState? = null,
    @Transient
    val timelineStateHolder: TimelineStateHolder? = null,
    @Transient
    val messages: List<ScaffoldMessage> = emptyList(),
)

fun State(
    route: Route,
) = State(
    sharedElementPrefix = route.sharedElementPrefix,
    timelineState = route.model?.let { model ->
        if (model !is FeedGenerator) return@let null
        val timeline = Timeline.Home.Feed.stub(feedGenerator = model)
        TimelineState(
            timeline = timeline,
            hasUpdates = false,
            tilingData = TilingState.Data(
                currentQuery = TimelineQuery(
                    data = CursorQuery.Data(
                        page = 0,
                        cursorAnchor = Clock.System.now(),
                    ),
                    timeline = timeline,
                ),
            ),
        )
    },
)

sealed class Action(val key: String) {

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: ScaffoldMessage,
    ) : Action(key = "SnackbarDismissed")

    data object ScrollToTop : Action(key = "ScrollToTop")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
