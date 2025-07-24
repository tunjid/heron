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

package com.tunjid.heron.profile

import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.profile.di.profileHandleOrId
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.avatarSharedElementKey
import com.tunjid.heron.scaffold.navigation.currentRoute
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource


@Serializable
data class State(
    val profile: Profile,
    val signedInProfileId: ProfileId? = null,
    val isSignedInProfile: Boolean = false,
    val viewerState: ProfileViewerState? = null,
    val avatarSharedElementKey: String,
    val commonFollowers: List<Profile> = emptyList(),
    @Transient
    val sourceIdsToHasUpdates: Map<String, Boolean> = emptyMap(),
    @Transient
    val stateHolders: List<ProfileScreenStateHolders> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)

fun State(route: Route) = State(
    avatarSharedElementKey = route.avatarSharedElementKey ?: "",
    profile = (route.model as? Profile) ?: stubProfile(
        did = ProfileId(route.profileHandleOrId.id),
        handle = ProfileHandle(route.profileHandleOrId.id),
        avatar = null,
    ),
)

sealed class ProfileScreenStateHolders {

    class Collections(
        val mutator: ProfileCollectionStateHolder
    ) : ProfileScreenStateHolders(),
        ProfileCollectionStateHolder by mutator

    class Timeline(
        val mutator: TimelineStateHolder,
    ) : ProfileScreenStateHolders(),
        TimelineStateHolder by mutator

    val key
        get() = when (this) {
            is Collections -> state.value.stringResource.toString()
            is Timeline -> state.value.timeline.sourceId
        }

    val tilingState: StateFlow<TilingState<*, *>>
        get() = when (this) {
            is Collections -> state
            is Timeline -> state
        }

    fun refresh() = when (this) {
        is Collections -> accept(
            TilingState.Action.Refresh
        )

        is Timeline -> accept(
            TimelineState.Action.Tile(
                tilingAction = TilingState.Action.Refresh
            )
        )
    }
}

typealias ProfileCollectionStateHolder = ActionStateMutator<TilingState.Action, StateFlow<ProfileCollectionState>>

data class ProfileCollectionState(
    val stringResource: StringResource,
    override val tilingData: TilingState.Data<ProfilesQuery, ProfileCollection>,
) : TilingState<ProfilesQuery, ProfileCollection>

sealed class ProfileCollection {

    val id
        get() = when (this) {
            is OfFeedGenerators -> feedGenerator.cid.id
            is OfLists -> list.cid.id
            is OfStarterPacks -> starterPack.cid.id
        }

    data class OfFeedGenerators(
        val feedGenerator: FeedGenerator,
    ) : ProfileCollection()

    data class OfStarterPacks(
        val starterPack: StarterPack,
    ) : ProfileCollection()

    data class OfLists(
        val list: FeedList,
    ) : ProfileCollection()
}

sealed class Action(val key: String) {

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class UpdatePageWithUpdates(
        val sourceId: String,
        val hasUpdates: Boolean,
    ) : Action(key = "UpdatePageWithUpdates")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: GenericUri?,
        val followedBy: GenericUri?,
    ) : Action(key = "ToggleViewerState")

    sealed class Navigate : Action(key = "Navigate"), NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(), NavigationAction by delegate

        data class ToAvatar(
            val profile: Profile,
            val avatarSharedElementKey: String?,
        ) : Navigate() {
            override val navigationMutation: NavigationMutation = {
                routeString(
                    path = "/profile/${profile.did.id}/avatar",
                    queryParams = mapOf(
                        "profile" to listOfNotNull(profile.toUrlEncodedBase64()),
                        "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                        referringRouteQueryParams(ReferringRouteOption.Current),
                    )
                )
                    .toRoute
                    .takeIf { it.id != currentRoute.id }
                    ?.let(navState::push)
                    ?: navState
            }
        }
    }
}
