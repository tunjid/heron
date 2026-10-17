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

package com.tunjid.heron.posts

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.DataQuery
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.data.repository.PostQuotesQuery
import com.tunjid.heron.posts.di.PostsRequest
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.state.TimelineState
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.asNoOpActionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val signedInProfileId: ProfileId? = null,
        val request: PostsRequest,
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        val stateHolders: List<PostsScreenStateHolders> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        internal operator fun invoke(
            request: PostsRequest,
        ): Immutable = Immutable(
            request = request,
            stateHolders = when (request) {
                is PostsRequest.Quotes -> PostQuotesQuery.Sort.entries.map { sort ->
                    PostsScreenStateHolders.QuotedPosts(
                        sort = sort,
                        mutator = PostsTimelineState.Immutable(
                            request = request,
                            tilingData = TilingState.Data(
                                currentQuery = PostQuotesQuery(
                                    sort = sort,
                                    profileId = request.profileHandleOrId,
                                    postRecordKey = request.postRecordKey,
                                    data = CursorQuery.Data(
                                        page = 0,
                                        cursorAnchor = Clock.System.now(),
                                    ),
                                ),
                            ),
                        ).asNoOpActionSuspendingStateMutator(),
                    )
                }
                is PostsRequest.Saved -> listOf(
                    PostsScreenStateHolders.Bookmarks(
                        mutator = PostsTimelineState.Immutable(
                            request = request,
                            tilingData = TilingState.Data(
                                currentQuery = DataQuery(
                                    data = CursorQuery.Data(
                                        page = 0,
                                        cursorAnchor = Clock.System.now(),
                                    ),
                                ),
                            ),
                        ).asNoOpActionSuspendingStateMutator(),
                    ),
                )
            },
        )
    }
}

@Stable
@Snapshottable
interface PostsTimelineState : TilingState<CursorQuery, TimelineItem> {
    @SnapshotSpec
    data class Immutable(
        val request: PostsRequest,
        @Transient
        override val tilingData: TilingState.Data<CursorQuery, TimelineItem>,
    ) : PostsTimelineState
}

typealias PostsTimelineMutator = ActionSuspendingStateMutator<TilingState.Action, PostsTimelineState>

@Stable
sealed class PostsScreenStateHolders : PostsTimelineMutator {

    @Stable
    class QuotedPosts(
        val sort: PostQuotesQuery.Sort,
        val mutator: PostsTimelineMutator,
    ) : PostsScreenStateHolders(),
        PostsTimelineMutator by mutator

    @Stable
    class Bookmarks(
        val mutator: PostsTimelineMutator,
    ) : PostsScreenStateHolders(),
        PostsTimelineMutator by mutator

    val key
        get() = when (this) {
            is Bookmarks -> "Bookmarks"
            is QuotedPosts -> sort.name
        }

    val isRefreshing: Boolean
        get() = when (this) {
            is Bookmarks -> state.isRefreshing
            is QuotedPosts -> state.isRefreshing
        }

    fun refresh() = when (this) {
        is Bookmarks -> accept(
            TilingState.Action.Refresh,
        )

        is QuotedPosts -> accept(
            TilingState.Action.Refresh,
        )
    }
}

sealed class Action(val key: String) {

    data class TogglePublicationSubscription(
        val publication: StandardPublication,
    ) : Action(key = "TogglePublicationSubscription")

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
