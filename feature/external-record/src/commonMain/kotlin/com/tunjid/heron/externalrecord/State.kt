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

package com.tunjid.heron.externalrecord

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.EventUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.repository.records.EventRsvpsQuery
import com.tunjid.heron.data.repository.records.StandardPublicationDocumentsQuery
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.isRefreshing
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.model
import com.tunjid.heron.ui.scaffold.navigation.sharedElementPrefix
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.treenav.strings.Route
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val sharedElementPrefix: String? = null,
        @Transient
        val record: Record.Embeddable.External? = null,
        @Transient
        val stateHolders: List<ExternalRecordStateHolders> = emptyList(),
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State

    companion object {
        operator fun invoke(
            route: Route,
        ): Immutable = Immutable(
            record = route.model<StandardPublication>()
                ?: route.model<Event>(),
            sharedElementPrefix = route.sharedElementPrefix,
        )
    }
}

@Stable
sealed class ExternalRecordStateHolders {

    @Stable
    class Documents(
        val mutator: DocumentsStateHolder,
    ) : ExternalRecordStateHolders(),
        DocumentsStateHolder by mutator

    @Stable
    class Rsvps(
        val mutator: RsvpsStateHolder,
    ) : ExternalRecordStateHolders(),
        RsvpsStateHolder by mutator

    @Stable
    class Comments(
        val mutator: CommentsStateHolder,
    ) : ExternalRecordStateHolders(),
        CommentsStateHolder by mutator

    val key
        get() = when (this) {
            is Documents -> "Documents"
            is Rsvps -> "Rsvps-${state.status.id}"
            is Comments -> "Comments"
        }

    val isRefreshing: Boolean
        get() = when (this) {
            is Documents -> state.isRefreshing
            is Rsvps -> state.isRefreshing
            is Comments -> false
        }

    fun refresh() = when (this) {
        is Documents -> accept(
            TilingState.Action.Refresh,
        )
        is Rsvps -> accept(
            TilingState.Action.Refresh,
        )
        is Comments -> accept(
            CommentsAction.Refresh,
        )
    }
}

typealias DocumentsStateHolder = ActionSuspendingStateMutator<TilingState.Action, DocumentsTilingState>

@Stable
@Snapshottable
interface DocumentsTilingState : TilingState<StandardPublicationDocumentsQuery, StandardDocument> {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val publicationUri: StandardPublicationUri,
        @Transient
        override val tilingData: TilingState.Data<StandardPublicationDocumentsQuery, StandardDocument> = TilingState.Data(
            currentQuery = StandardPublicationDocumentsQuery(
                publicationUri = publicationUri,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                ),
            ),
        ),
    ) : DocumentsTilingState

    companion object {
        operator fun invoke(
            publicationUri: StandardPublicationUri,
        ): Immutable = Immutable(
            publicationUri = publicationUri,
        )
    }
}

typealias RsvpsStateHolder = ActionSuspendingStateMutator<TilingState.Action, RsvpsTilingState>

@Stable
@Snapshottable
interface RsvpsTilingState : TilingState<EventRsvpsQuery, Rsvp> {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val eventUri: EventUri,
        val status: Rsvp.Status,
        @Transient
        override val tilingData: TilingState.Data<EventRsvpsQuery, Rsvp> = TilingState.Data(
            currentQuery = EventRsvpsQuery(
                eventUri = eventUri,
                status = status,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                ),
            ),
        ),
    ) : RsvpsTilingState

    companion object {
        operator fun invoke(
            eventUri: EventUri,
            status: Rsvp.Status,
        ): Immutable = Immutable(
            eventUri = eventUri,
            status = status,
        )
    }
}

typealias CommentsStateHolder = ActionSuspendingStateMutator<CommentsAction, CommentsState>

@Stable
@Snapshottable
interface CommentsState {

    @SnapshotSpec
    data class Immutable(
        val postUri: PostUri,
        val items: List<TimelineItem> = emptyList(),
    ) : CommentsState

    companion object {
        operator fun invoke(
            postUri: PostUri,
        ): Immutable = Immutable(
            postUri = postUri,
        )
    }
}

sealed class CommentsAction {
    data object Refresh : CommentsAction()
}

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

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class RecordInteraction(
        val interaction: PostAction.RecordInteraction,
    ) : Action(key = "RecordInteraction")

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
