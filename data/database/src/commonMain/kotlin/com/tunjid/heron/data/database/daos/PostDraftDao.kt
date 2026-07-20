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

package com.tunjid.heron.data.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.DraftId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PostDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDraftDao {

    @Transaction
    @Upsert
    suspend fun upsertPostDrafts(
        entities: List<PostDraftEntity>,
    )

    @Transaction
    @Query(
        """
        SELECT * FROM postDrafts
        WHERE authorId = :authorId
        ORDER BY updatedAt DESC
        LIMIT :limit
        OFFSET :offset
    """,
    )
    fun postDrafts(
        authorId: ProfileId,
        offset: Long,
        limit: Long,
    ): Flow<List<PostDraftEntity>>

    @Transaction
    @Query(
        """
        DELETE FROM postDrafts
        WHERE id = :id
    """,
    )
    suspend fun deletePostDraft(
        id: DraftId,
    )
}
