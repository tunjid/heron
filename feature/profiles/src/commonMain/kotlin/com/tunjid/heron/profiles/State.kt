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

package com.tunjid.heron.profiles

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.profiles.di.load
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.treenav.strings.Route
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val signedInProfileId: ProfileId? = null,
    val load: Load,
    override val tilingData: TilingState.Data<CursorQuery, ProfileWithViewerState>,
    @Transient
    val messages: List<Memo> = emptyList(),
) : TilingState<CursorQuery, ProfileWithViewerState>

fun State(route: Route) = State(
    load = route.load,
    tilingData = TilingState.Data(
        currentQuery = when (val load = route.load) {
            is Load.Post -> PostDataQuery(
                profileId = load.profileId,
                postRecordKey = load.postRecordKey,
                data = CursorQuery.defaultStartData(),
            )

            is Load.Profile -> ProfilesQuery(
                profileId = load.profileId,
                data = CursorQuery.defaultStartData(),
            )
            Load.Moderation.Blocks,
            Load.Moderation.Mutes,
            -> DataQuery(
                data = CursorQuery.defaultStartData(),
            )
        },
    ),
)

@Serializable
sealed class Load {
    @Serializable
    sealed class Post : Load() {
        abstract val profileId: Id.Profile
        abstract val postRecordKey: RecordKey

        @Serializable
        data class Likes(
            override val postRecordKey: RecordKey,
            override val profileId: Id.Profile,
        ) : Post()

        @Serializable
        data class Reposts(
            override val postRecordKey: RecordKey,
            override val profileId: Id.Profile,
        ) : Post()
    }

    @Serializable
    sealed class Profile : Load() {
        abstract val profileId: Id.Profile

        @Serializable
        data class Followers(
            override val profileId: Id.Profile,
        ) : Profile()

        @Serializable
        data class Following(
            override val profileId: Id.Profile,
        ) : Profile()
    }

    @Serializable
    sealed class Moderation : Load() {
        @Serializable
        data object Blocks : Moderation()

        @Serializable
        data object Mutes : Moderation()
    }
}

sealed class Action(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : Action(key = "Load")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: FollowUri?,
        val followedBy: FollowUri?,
    ) : Action(key = "ToggleViewerState")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

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
