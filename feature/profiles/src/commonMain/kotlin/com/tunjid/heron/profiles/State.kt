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
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    val signedInProfileId: ProfileId? = null,
    override val tilingData: TilingState.Data<CursorQuery, ProfileWithViewerState>,
    @Transient
    val messages: List<Memo> = emptyList(),
) : TilingState<CursorQuery, ProfileWithViewerState>

sealed class Load {
    sealed class Post : Load() {
        abstract val profileId: Id.Profile
        abstract val postRecordKey: RecordKey

        data class Likes(
            override val postRecordKey: RecordKey,
            override val profileId: Id.Profile,
        ) : Post()

        data class Reposts(
            override val postRecordKey: RecordKey,
            override val profileId: Id.Profile,
        ) : Post()
    }

    sealed class Profile : Load() {
        abstract val profileId: Id.Profile

        data class Followers(
            override val profileId: Id.Profile,
        ) : Profile()

        data class Following(
            override val profileId: Id.Profile,
        ) : Profile()
    }

    sealed class Moderation : Load() {
        data object Blocks : Moderation()
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
