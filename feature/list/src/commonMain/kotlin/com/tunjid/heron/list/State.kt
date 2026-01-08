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
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ListMemberQuery
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.model
import com.tunjid.heron.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.timeline.utilities.MutedWordUpdateAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.treenav.strings.Route
import kotlin.time.Clock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
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
    val timelineState: TimelineState? = null,
    @Transient
    val stateHolders: List<ListScreenStateHolders> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

fun State(
    route: Route,
) = State(
    sharedElementPrefix = route.sharedElementPrefix,
    timelineState = route.model?.let { model ->
        when (model) {
            is FeedList -> {
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
                            timeline = timeline,
                        ),
                    ),
                )
            }

            is StarterPack -> when (val starterPackList = model.list) {
                null -> null
                else -> {
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
                                timeline = timeline,
                            ),
                        ),
                    )
                }
            }

            else -> null
        }
    },
)

sealed class ListScreenStateHolders {

    class Members(
        val mutator: MembersStateHolder,
    ) : ListScreenStateHolders(),
        MembersStateHolder by mutator

    class Timeline(
        val mutator: TimelineStateHolder,
    ) : ListScreenStateHolders(),
        TimelineStateHolder by mutator

    val key
        get() = when (this) {
            is Members -> "Members"
            is Timeline -> "Timeline"
        }

    val tilingState: StateFlow<TilingState<*, *>>
        get() = when (this) {
            is Members -> state
            is Timeline -> state
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

data class MemberState(
    val signedInProfileId: ProfileId?,
    override val tilingData: TilingState.Data<ListMemberQuery, ListMember>,
) : TilingState<ListMemberQuery, ListMember>

typealias MembersStateHolder = ActionStateMutator<TilingState.Action, StateFlow<MemberState>>

sealed class Action(val key: String) {

    data class UpdateMutedWord(
        override val mutedWordPreference: List<MutedWordPreference>,
    ) : Action(key = "UpdateMutedWord"),
        MutedWordUpdateAction

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

    data class UpdateFeedListStatus(
        val update: Timeline.Update,
    ) : Action(key = "UpdateFeedGeneratorStatus")

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
