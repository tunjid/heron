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

package com.tunjid.heron.timeline.state

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Record as HeronRecord
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import com.tunjid.tiler.distinctBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

typealias RecordStateHolder<T> = ActionSuspendingStateMutator<TilingState.Action, RecordState<T>>

@Stable
@Snapshottable
interface RecordState<T : HeronRecord> : TilingState<ProfilesQuery, T> {
    @SnapshotSpec
    data class Immutable<T : HeronRecord>(
        val stringResource: StringResource,
        @Transient
        override val tilingData: TilingState.Data<ProfilesQuery, T>,
    ) : RecordState<T>
}

inline fun <reified T : HeronRecord> CoroutineScope.recordStateHolder(
    profileId: ProfileId,
    stringResource: StringResource,
    crossinline itemId: (T) -> Any,
    crossinline cursorListLoader: (ProfilesQuery, Cursor) -> Flow<CursorList<T>>,
): RecordStateHolder<T> {
    val state: RecordState<T> = RecordState.SnapshotMutable<T>(
        stringResource = stringResource,
        tilingData = TilingState.Data(
            currentQuery = ProfilesQuery(
                profileId = profileId,
                data = CursorQuery.defaultStartData(),
            ),
        ),
    )
    return actionSuspendingStateMutator<TilingState.Action, RecordState<T>>(
        initialState = state,
        producer = { state, actions ->
            actions.tilingMutations<ProfilesQuery, T, RecordState<T>>(
                state = state,
                updateQueryData = { copy(data = it) },
                refreshQuery = { copy(data = data.reset()) },
                cursorListLoader = { query, cursor ->
                    cursorListLoader(query, cursor)
                },
                onNewItems = { items ->
                    items.distinctBy(itemId)
                },
            )
        },
    )
}
