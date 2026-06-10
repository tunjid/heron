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
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.PostDataQuery
import com.tunjid.heron.posts.di.PostsRequest
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State : TilingState<PostDataQuery, TimelineItem> {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val signedInProfileId: ProfileId? = null,
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        override val tilingData: TilingState.Data<PostDataQuery, TimelineItem> = TilingState.Data(
            currentQuery = PostDataQuery(
                profileId = ProfileHandle(""),
                postRecordKey = RecordKey(""),
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                ),
            ),
        ),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        internal operator fun invoke(
            request: PostsRequest,
        ): Immutable = Immutable(
            tilingData = TilingState.Data(
                currentQuery = when (request) {
                    is PostsRequest.Quotes -> PostDataQuery(
                        profileId = request.profileHandleOrId,
                        postRecordKey = request.postRecordKey,
                        data = CursorQuery.Data(
                            page = 0,
                            cursorAnchor = Clock.System.now(),
                        ),
                    )
                    PostsRequest.Saved -> PostDataQuery(
                        profileId = ProfileHandle(""),
                        postRecordKey = RecordKey(""),
                        data = CursorQuery.Data(
                            page = 0,
                            cursorAnchor = Clock.System.now(),
                        ),
                    )
                },
            ),
        )
    }
}

val State.isRefreshing: Boolean
    get() = tilingData.status is TilingState.Status.Refreshing

sealed class Action(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : Action("Tile")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

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
