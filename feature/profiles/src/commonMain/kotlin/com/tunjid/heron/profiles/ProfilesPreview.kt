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

package com.tunjid.heron.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.profiles.di.Route as ProfilesRoute
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.preview.RoutePreview
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.treenav.strings.routeOf
import kotlin.time.Instant

@Preview
@Composable
internal fun ProfilesPreview() {
    val profileId = ProfileId("did:example:123")
    RoutePreview(
        route = routeOf(path = "/profile/alice.bsky.social/followers"),
        routeStateHolder = ActualProfilesViewModel(
            mutator = State.Immutable(
                load = Load.Profile.Followers(
                    profileId = profileId,
                ),
                tilingData = TilingState.Data(
                    currentQuery = ProfilesQuery(
                        profileId = profileId,
                        data = CursorQuery.Data(
                            page = 0,
                            cursorAnchor = Instant.DISTANT_PAST,
                        ),
                    ),
                ),
            ).asNoOpActionSuspendingStateMutator(),
            scope = rememberCoroutineScope(),
        ),
        render = { route, paneScaffoldState ->
            ProfilesRoute(
                route = route,
                paneScaffoldState = paneScaffoldState,
            )
        },
    )
}
