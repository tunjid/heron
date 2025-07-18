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
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.timeline.state.TimelineStateHolders
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.currentRoute
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.emptyTiledList
import com.tunjid.treenav.push
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
    val timelineStateHolders: TimelineStateHolders = TimelineStateHolders(),
    @Transient
    val collectionStateHolders: List<ProfileCollectionStateHolder> = emptyList(),
    @Transient
    val messages: List<String> = emptyList(),
)

typealias ProfileCollectionStateHolder = ActionStateMutator<ProfilesQuery, StateFlow<ProfileCollectionState>>

data class ProfileCollectionState(
    val stringResource: StringResource,
    val currentQuery: ProfilesQuery,
    val items: TiledList<ProfilesQuery, ProfileCollection> = emptyTiledList(),
)

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
        data object Pop : Navigate(), NavigationAction by NavigationAction.Common.Pop

        data class DelegateTo(
            val delegate: NavigationAction.Common,
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