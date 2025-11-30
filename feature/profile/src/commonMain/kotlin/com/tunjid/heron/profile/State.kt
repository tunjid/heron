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

import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.profile.ProfileScreenStateHolders.LabelerSettings
import com.tunjid.heron.profile.ProfileScreenStateHolders.LabelerSettings.Settings
import com.tunjid.heron.profile.ProfileScreenStateHolders.Records
import com.tunjid.heron.profile.di.profileHandleOrId
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.avatarSharedElementKey
import com.tunjid.heron.scaffold.navigation.currentRoute
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val timelineRecordUrisToPinnedStatus: Map<RecordUri?, Boolean> = emptyMap(),
    val subscribedLabelerProfileIds: Set<ProfileId> = emptySet(),
    @Transient
    val recentConversations: List<Conversation> = emptyList(),
    @Transient
    val sourceIdsToHasUpdates: Map<String, Boolean> = emptyMap(),
    @Transient
    val stateHolders: List<ProfileScreenStateHolders> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(route: Route) = State(
    avatarSharedElementKey = route.avatarSharedElementKey ?: "",
    profile = (route.model as? Profile) ?: stubProfile(
        did = ProfileId(route.profileHandleOrId.id),
        handle = ProfileHandle(route.profileHandleOrId.id),
        avatar = null,
    ),
)

val State.isSubscribedToLabeler
    get() = profile.isLabeler && subscribedLabelerProfileIds.contains(profile.did)

sealed class ProfileScreenStateHolders {

    sealed class Records<T : Record>(
        private val mutator: RecordStateHolder<T>,
    ) : ProfileScreenStateHolders(),
        RecordStateHolder<T> by mutator {

        class Feeds(
            mutator: RecordStateHolder<FeedGenerator>,
        ) : Records<FeedGenerator>(mutator)

        class Lists(
            mutator: RecordStateHolder<FeedList>,
        ) : Records<FeedList>(mutator)

        class StarterPacks(
            mutator: RecordStateHolder<StarterPack>,
        ) : Records<StarterPack>(mutator)
    }

    class Timeline(
        private val mutator: TimelineStateHolder,
    ) : ProfileScreenStateHolders(),
        TimelineStateHolder by mutator

    class LabelerSettings(
        private val mutator: LabelerSettingsStateHolder,
    ) : ProfileScreenStateHolders(),
        LabelerSettingsStateHolder by mutator {

        data class Settings(
            val subscribed: Boolean = false,
            val labelSettings: List<LabelSetting> = emptyList(),
        )

        data class LabelSetting(
            val definition: Label.Definition,
            val visibility: Label.Visibility,
        )
    }

    val key
        get() = when (this) {
            is Records.Feeds -> "Feeds"
            is Records.Lists -> "Lists"
            is Records.StarterPacks -> "StarterPacks"
            is Timeline -> state.value.timeline.sourceId
            is LabelerSettings -> "LabelerSettings"
        }

    fun refresh() = when (this) {
        is Records<*> -> accept(
            TilingState.Action.Refresh,
        )

        is Timeline -> accept(
            TimelineState.Action.Tile(
                tilingAction = TilingState.Action.Refresh,
            ),
        )
        is LabelerSettings -> Unit
    }
}

val ProfileScreenStateHolders?.isRefreshing
    get() = when (this) {
        is Records<*> -> state.map { it.isRefreshing }
        is ProfileScreenStateHolders.Timeline -> state.map { it.isRefreshing }
        is LabelerSettings,
        null,
        -> flowOf(false)
    }

val ProfileScreenStateHolders?.canRefresh
    get() = when (this) {
        is Records<*>,
        is ProfileScreenStateHolders.Timeline,
        -> true
        is LabelerSettings,
        null,
        -> false
    }

typealias RecordStateHolder<T> = ActionStateMutator<TilingState.Action, StateFlow<RecordState<T>>>
typealias LabelerSettingsStateHolder = ActionStateMutator<LabelerSettings.LabelSetting, StateFlow<Settings>>

data class RecordState<T : Record>(
    val stringResource: StringResource,
    override val tilingData: TilingState.Data<ProfilesQuery, T>,
) : TilingState<ProfilesQuery, T>

sealed class Action(val key: String) {

    data class BioLinkClicked(
        val target: LinkTarget,
    ) : Action(key = "BioLinkClicked")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class UpdatePageWithUpdates(
        val sourceId: String,
        val hasUpdates: Boolean,
    ) : Action(key = "UpdatePageWithUpdates")

    data class UpdateThreadGate(
        val summary: ThreadGate.Summary,
    ) : Action(key = "UpdateThreadGate")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: GenericUri?,
        val followedBy: GenericUri?,
    ) : Action(key = "ToggleViewerState")

    data class UpdatePreferences(
        val update: Timeline.Update,
    ) : Action(key = "UpdatePreferences")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate

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
                    ),
                )
                    .toRoute
                    .takeIf { it.id != currentRoute.id }
                    ?.let(navState::push)
                    ?: navState
            }
        }
    }
}
