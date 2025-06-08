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

import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.pop
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class State(
    val signedInProfileId: ProfileId? = null,
    val currentQuery: CursorQuery,
    @Transient
    val profiles: TiledList<CursorQuery, ProfileWithViewerState> = emptyTiledList(),
    @Transient
    val messages: List<String> = emptyList(),
)

sealed class Load {
    abstract val profileId: Id.Profile

    sealed class Post : Load() {
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

        data class Followers(
            override val profileId: Id.Profile,
        ) : Profile()

        data class Following(
            override val profileId: Id.Profile,
        ) : Profile()
    }

}

sealed class Action(val key: String) {

    data class LoadAround(
        val query: CursorQuery,
    ) : Action("LoadAround")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: GenericUri?,
        val followedBy: GenericUri?,
    ) : Action(key = "ToggleViewerState")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate() {
            override val navigationMutation: NavigationMutation = {
                navState.pop()
            }
        }

        data class DelegateTo(
            val delegate: NavigationAction.Common,
        ) : Navigate(), NavigationAction by delegate
    }
}