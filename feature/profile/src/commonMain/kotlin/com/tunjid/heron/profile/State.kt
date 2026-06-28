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

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labelers
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.sourceId
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.BlockUri
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.profile.ProfileScreenStateHolders.LabelerSettings
import com.tunjid.heron.profile.ProfileScreenStateHolders.LabelerSettings.Settings
import com.tunjid.heron.profile.ProfileScreenStateHolders.Records
import com.tunjid.heron.profile.di.profileHandleOrId
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.state.RecordStateHolder
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.scaffold.navigation.avatarSharedElementKey
import com.tunjid.heron.ui.scaffold.navigation.currentRoute
import com.tunjid.heron.ui.scaffold.navigation.model
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString
import kotlin.collections.any
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val profile: Profile,
        val signedInProfileId: ProfileId? = null,
        val isSignedInProfile: Boolean = false,
        val viewerState: ProfileViewerState? = null,
        val avatarSharedElementKey: String,
        val currentPage: Int = 0,
        val commonFollowers: List<Profile> = emptyList(),
        val timelineRecordUrisToPinnedStatus: Map<RecordUri?, Boolean> = emptyMap(),
        val subscribedLabelers: Labelers = emptyList(),
        @Transient
        val supportedApps: List<AtmosphereApp> = emptyList(),
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        val sourceIdsToHasUpdates: Map<String, Boolean> = emptyMap(),
        @Transient
        val stateHolders: List<ProfileScreenStateHolders> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(route: Route): Immutable = Immutable(
            avatarSharedElementKey = route.avatarSharedElementKey ?: "",
            profile = route.model<Profile>() ?: stubProfile(
                did = ProfileId(route.profileHandleOrId.id),
                handle = ProfileHandle(route.profileHandleOrId.id),
                avatar = null,
            ),
        )
    }
}

val State.isSubscribedToLabeler
    get() = profile.isLabeler && subscribedLabelers.any { it.creator.did == profile.did }

@Stable
sealed class ProfileScreenStateHolders {

    @Stable
    sealed class Records<T : Record>(
        private val mutator: RecordStateHolder<T>,
    ) : ProfileScreenStateHolders(),
        RecordStateHolder<T> by mutator {

        @Stable
        class Feeds(
            mutator: RecordStateHolder<FeedGenerator>,
        ) : Records<FeedGenerator>(mutator)

        @Stable
        class Lists(
            mutator: RecordStateHolder<FeedList>,
        ) : Records<FeedList>(mutator)

        @Stable
        class StarterPacks(
            mutator: RecordStateHolder<StarterPack>,
        ) : Records<StarterPack>(mutator)

        @Stable
        class Documents(
            mutator: RecordStateHolder<StandardDocument>,
        ) : Records<StandardDocument>(mutator)

        @Stable
        class Publications(
            mutator: RecordStateHolder<StandardPublication>,
        ) : Records<StandardPublication>(mutator)
    }

    @Stable
    class Timeline(
        private val mutator: TimelineStateHolder,
    ) : ProfileScreenStateHolders(),
        TimelineStateHolder by mutator

    @Stable
    class LabelerSettings(
        private val mutator: LabelerSettingsStateHolder,
    ) : ProfileScreenStateHolders(),
        LabelerSettingsStateHolder by mutator {

        @Stable
        @Snapshottable
        interface Settings {
            @SnapshotSpec
            data class Immutable(
                val subscribed: Boolean = false,
                val labelSettings: List<LabelSetting> = emptyList(),
            ) : Settings
        }

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
            is Records.Documents -> "Documents"
            is Records.Publications -> "Publications"
            is Timeline -> state.timeline.sourceId
            is LabelerSettings -> "LabelerSettings"
        }

    val tab
        get() = when (this) {
            is Records.Feeds -> ProfileTab.Bluesky.FeedGenerators.All
            is Records.Lists -> ProfileTab.Bluesky.Lists.All
            is Records.StarterPacks -> ProfileTab.Bluesky.StarterPacks
            is Records.Documents -> ProfileTab.StandardSite.Documents
            is Records.Publications -> ProfileTab.StandardSite.Publications
            is Timeline -> when (val timeline = state.timeline) {
                is com.tunjid.heron.data.core.models.Timeline.Home.Feed -> ProfileTab.Bluesky.FeedGenerators.FeedGenerator(
                    timeline.feedGenerator.uri,
                )

                is com.tunjid.heron.data.core.models.Timeline.Profile -> when (timeline.type) {
                    com.tunjid.heron.data.core.models.Timeline.Profile.Type.Posts -> ProfileTab.Bluesky.Posts.Standard
                    com.tunjid.heron.data.core.models.Timeline.Profile.Type.Replies -> ProfileTab.Bluesky.Posts.Replies
                    com.tunjid.heron.data.core.models.Timeline.Profile.Type.Likes -> ProfileTab.Bluesky.Posts.Likes
                    com.tunjid.heron.data.core.models.Timeline.Profile.Type.Media -> ProfileTab.Bluesky.Posts.Media
                    com.tunjid.heron.data.core.models.Timeline.Profile.Type.Videos -> ProfileTab.Bluesky.Posts.Videos
                }
                is com.tunjid.heron.data.core.models.Timeline.Home.Following,
                is com.tunjid.heron.data.core.models.Timeline.Home.List,
                is com.tunjid.heron.data.core.models.Timeline.StarterPack,
                -> null
            }
            is LabelerSettings -> null
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
        is Records<*> -> state.isRefreshing
        is ProfileScreenStateHolders.Timeline -> state.isRefreshing
        is LabelerSettings,
        null,
        -> false
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

typealias LabelerSettingsStateHolder = ActionSuspendingStateMutator<LabelerSettings.LabelSetting, Settings>

sealed class Action(val key: String) {

    data class BioLinkClicked(
        val target: LinkTarget,
    ) : Action(key = "BioLinkClicked")

    sealed class UpdateLiveStatus(key: String) : Action(key) {
        data class GoLive(
            val signedInProfileId: ProfileId,
            val streamUrl: String,
            val duration: Int,
        ) : UpdateLiveStatus(key = "GoLive")

        data class EndLive(
            val signedInProfileId: ProfileId,
        ) : UpdateLiveStatus(key = "EndLive")
    }

    sealed class Moderation(
        key: String,
    ) : Action(key)

    sealed class Block : Moderation(key = "Block") {
        data class Add(
            val signedInProfileId: ProfileId,
            val profileId: ProfileId,
        ) : Block()

        data class Remove(
            val signedInProfileId: ProfileId,
            val profileId: ProfileId,
            val blockUri: BlockUri,
        ) : Block()
    }

    sealed class Mute : Moderation(key = "Mute") {
        data class Add(
            val signedInProfileId: ProfileId,
            val profileId: ProfileId,
        ) : Mute()

        data class Remove(
            val signedInProfileId: ProfileId,
            val profileId: ProfileId,
        ) : Mute()
    }

    data class PageChanged(
        val page: Int,
    ) : Action(key = "PageChanged")

    sealed class TogglePublicationSubscription : Action(key = "TogglePublicationSubscription") {
        data class Subscribe(
            val publicationUri: StandardPublicationUri,
        ) : TogglePublicationSubscription()

        data class Unsubscribe(
            val subscriptionUri: StandardSubscriptionUri,
        ) : TogglePublicationSubscription()
    }

    data class DeleteRecord(
        val recordUri: RecordUri,
    ) : Action(key = "DeleteRecord")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class UpdatePageWithUpdates(
        val sourceId: String,
        val hasUpdates: Boolean,
    ) : Action(key = "UpdatePageWithUpdates")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: FollowUri?,
        val followedBy: FollowUri?,
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
