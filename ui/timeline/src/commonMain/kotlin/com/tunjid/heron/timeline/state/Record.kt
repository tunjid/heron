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

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.distinctBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.StringResource

typealias RecordStateHolder<T> = ActionStateMutator<TilingState.Action, StateFlow<RecordState<T>>>

data class RecordState<T : Record>(
    val stringResource: StringResource,
    override val tilingData: TilingState.Data<ProfilesQuery, T>,
) : TilingState<ProfilesQuery, T>

fun <T : Record> CoroutineScope.recordStateHolder(
    profileId: ProfileId,
    stringResource: StringResource,
    itemId: (T) -> Any,
    cursorListLoader: (ProfilesQuery, Cursor) -> Flow<CursorList<T>>,
): RecordStateHolder<T> = actionStateFlowMutator(
    initialState = RecordState(
        stringResource = stringResource,
        tilingData = TilingState.Data(
            currentQuery = ProfilesQuery(
                profileId = profileId,
                data = CursorQuery.defaultStartData(),
            ),
        ),
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream {
            type().flow
                .tilingMutations(
                    currentState = { state() },
                    updateQueryData = { copy(data = it) },
                    refreshQuery = { copy(data = data.reset()) },
                    cursorListLoader = cursorListLoader,
                    onNewItems = { items ->
                        items.distinctBy(itemId)
                    },
                    onTilingDataUpdated = { copy(tilingData = it) },
                )
        }
    },
)
