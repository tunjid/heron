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

package com.tunjid.heron.compose.drafts

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.types.DraftId
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.reset
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Pagination query for the signed in user's post drafts. A dedicated type (rather than the shared
 * `DataQuery`) because tiling resolves the query type reified at runtime.
 */
@Serializable
data class DraftQuery(
    override val data: CursorQuery.Data,
) : CursorQuery

@Stable
@Snapshottable
interface DraftsState : TilingState<DraftQuery, Post.Draft> {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        @Transient
        override val tilingData: TilingState.Data<DraftQuery, Post.Draft> = TilingState.Data(
            currentQuery = DraftQuery(
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = Clock.System.now(),
                ),
            ),
        ),
    ) : DraftsState
}

internal fun DraftQuery.updateData(newData: CursorQuery.Data): DraftQuery =
    copy(data = newData)

internal fun DraftQuery.refresh(): DraftQuery =
    copy(data = data.reset())

sealed class DraftsAction(val key: String) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : DraftsAction("Tile")

    data class Delete(
        val draftId: DraftId,
    ) : DraftsAction("Delete")
}
