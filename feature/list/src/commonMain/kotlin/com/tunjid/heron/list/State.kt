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

package com.tunjid.heron.list

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.ListMember
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.ui.coroutines.noOpActionSuspendingStateMutator
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.strings.Route
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.filterIsInstance
import kotlin.collections.firstOrNull
import kotlin.collections.listOfNotNull
import kotlin.collections.map
import kotlin.collections.take
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val creator: Profile? = null,
        val sharedElementPrefix: String? = null,
        @Transient
        val listStatus: Timeline.Home.Status = Timeline.Home.Status.None,
        @Transient
        val signedInProfileId: ProfileId? = null,
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        val recentConversations: List<Conversation> = emptyList(),
        @Transient
        val stateHolders: List<ListScreenStateHolders> = emptyList(),
        @Transient
        val recentLists: List<FeedList> = emptyList(),
        @Transient
        val isOnProfilesTab: Boolean = false,
        @Transient
        val suggestedProfiles: List<Profile> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(
            route: Route,
        ): Immutable = Immutable(
            sharedElementPrefix = route.sharedElementPrefix,
            stateHolders = listOfNotNull(
                route.model<FeedList>()?.let { model ->
                    val timeline = Timeline.Home.List.stub(list = model)
                    TimelineState(
                        timeline = timeline,
                        hasUpdates = false,
                        tilingData = TilingState.Data(
                            currentQuery = TimelineQuery(
                                data = CursorQuery.Data(
                                    page = 0,
                                    cursorAnchor = Clock.System.now(),
                                ),
                                source = timeline.source,
                            ),
                        ),
                    )
                },
                route.model<StarterPack>()?.let { model ->
                    val starterPackList = model.list ?: return@let null
                    val timeline = Timeline.StarterPack.stub(
                        starterPack = model,
                        list = starterPackList,
                    )
                    TimelineState(
                        timeline = timeline,
                        hasUpdates = false,
                        tilingData = TilingState.Data(
                            currentQuery = TimelineQuery(
                                data = CursorQuery.Data(
                                    page = 0,
                                    cursorAnchor = Clock.System.now(),
                                ),
                                source = timeline.source,
                            ),
                        ),
                    )
                },
            ).map {
                ListScreenStateHolders.Timeline(
                    noOpActionSuspendingStateMutator(it),
                )
            }
                .take(1),
        )
    }
}

val State.timelineState
    get() = stateHolders
        .filterIsInstance<ListScreenStateHolders.Timeline>()
        .firstOrNull()
        ?.state

@Stable
sealed class ListScreenStateHolders {

    @Stable
    class Members(
        val mutator: MembersStateHolder,
    ) : ListScreenStateHolders(),
        MembersStateHolder by mutator

    @Stable
    class Timeline(
        val mutator: TimelineStateHolder,
    ) : ListScreenStateHolders(),
        TimelineStateHolder by mutator

    val key
        get() = when (this) {
            is Members -> "Members"
            is Timeline -> "Timeline"
        }

    val isRefreshing: Boolean
        get() = when (this) {
            is Members -> state.isRefreshing
            is Timeline -> state.isRefreshing
        }

    fun refresh() = when (this) {
        is Members -> accept(
            TilingState.Action.Refresh,
        )

        is Timeline -> accept(
            TimelineState.Action.Tile(
                tilingAction = TilingState.Action.Refresh,
            ),
        )
    }
}

@Stable
@Snapshottable
interface MemberState : TilingState<ListMemberQuery, ListMember> {

    @SnapshotSpec
    data class Immutable(
        val signedInProfileId: ProfileId?,
        val listUri: ListUri,
        @Transient
        override val tilingData: TilingState.Data<ListMemberQuery, ListMember>,
    ) : MemberState

    companion object {
        operator fun invoke(
            signedInProfileId: ProfileId?,
            listUri: ListUri,
            tilingData: TilingState.Data<ListMemberQuery, ListMember>,
        ): Immutable = Immutable(
            signedInProfileId = signedInProfileId,
            listUri = listUri,
            tilingData = tilingData,
        )
    }
}

typealias MembersStateHolder = ActionSuspendingStateMutator<TilingState.Action, MemberState.SnapshotMutable>

sealed class Action(val key: String) {

    data class BlockAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "BlockAccount")

    data class MuteAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "MuteAccount")

    data class DeleteRecord(
        val recordUri: RecordUri,
    ) : Action(key = "DeleteRecord")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: FollowUri?,
        val followedBy: FollowUri?,
    ) : Action(key = "ToggleViewerState")

    data class AddListMember(
        val profileId: ProfileId,
        val listUri: ListUri,
    ) : Action(key = "AddListMember")

    data class SearchProfiles(
        val query: String,
    ) : Action(key = "SearchProfiles")

    data class CurrentPageChanged(
        val currentPage: Int,
    ) : Action(key = "CurrentPageChanged")

    data class UpdateFeedListStatus(
        val update: Timeline.Update,
    ) : Action(key = "UpdateFeedGeneratorStatus")

    data object UpdateRecentLists : Action(key = "UpdateRecentLists")

    data object UpdateRecentConversations : Action(key = "UpdateRecentConversations")

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
