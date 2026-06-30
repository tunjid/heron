/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.profile.di.Route as ProfileRoute
import com.tunjid.heron.timeline.state.preview.stubTimelineStateHolder
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf
import kotlin.time.Instant

@Preview
@Composable
internal fun ProfilePreview() {
    val profileId = ProfileId("did:example:123")
    RoutePreview(
        route = routeOf(path = "/profile/alice.bsky.social"),
        routeStateHolder = ActualProfileViewModel(
            mutator = State.Immutable(
                profile = stubProfile(
                    profileId,
                    ProfileHandle("alice.bsky.social"),
                ),
                avatarSharedElementKey = "preview",
                stateHolders = listOf(
                    Timeline.Profile.Type.Posts,
                    Timeline.Profile.Type.Replies,
                    Timeline.Profile.Type.Media,
                    Timeline.Profile.Type.Videos,
                ).map { type ->
                    ProfileScreenStateHolders.Timeline(
                        mutator = stubTimelineStateHolder(
                            timeline = Timeline.Profile(
                                profileId = profileId,
                                type = type,
                                lastRefreshed = Instant.parse("2024-01-01T00:00:00Z"),
                                itemsAvailable = 0,
                                presentation = Timeline.Presentation.Text.WithEmbed,
                            ),
                        ),
                    )
                },
            ).asNoOpActionSuspendingStateMutator(),
            scope = rememberCoroutineScope(),
        ),
        render = { route, paneScaffoldState ->
            ProfileRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}
