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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ThreadGateUri
import com.tunjid.heron.data.database.entities.ListMemberEntity
import com.tunjid.heron.data.database.entities.PopulatedThreadGateEntity
import com.tunjid.heron.data.database.entities.ThreadGateAllowedListEntity
import com.tunjid.heron.data.database.entities.ThreadGateEntity
import com.tunjid.heron.data.database.entities.ThreadGateHiddenPostEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.TimelinePreferencesEntity
import com.tunjid.heron.data.database.entities.fetchedAtPartial
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface ThreadGateDao {

    @Transaction
    @Upsert
    suspend fun upsertThreadGates(
        entities: List<ThreadGateEntity>,
    )

    @Transaction
    @Upsert
    suspend fun upsertThreadGateAllowedLists(
        entities: List<ThreadGateAllowedListEntity>,
    )

    @Transaction
    @Upsert
    suspend fun upsertThreadGateHiddenPosts(
        entities: List<ThreadGateHiddenPostEntity>,
    )

    @Transaction
    @Query(
        """
        SELECT * FROM threadGates
        WHERE gatedPostUri IN (:postUris)
    """,
    )
    suspend fun threadGates(
        postUris: Collection<PostUri>,
    ): List<PopulatedThreadGateEntity>

    @Transaction
    @Query(
        """
        DELETE FROM threadGates
        WHERE gatedPostUri IN (:postUris)
    """,
    )
    suspend fun deleteThreadGates(
        postUris: Collection<PostUri>,
    )

    @Transaction
    @Query(
        """
        DELETE FROM threadGateAllowedLists
        WHERE threadGateUri IN (:threadGateUris)
    """,
    )
    suspend fun deleteThreadGateAllowedLists(
        threadGateUris: Collection<ThreadGateUri>,
    )

    @Transaction
    @Query(
        """
        DELETE FROM threadGateHiddenPosts
        WHERE threadGateUri IN (:threadGateUris)
    """,
    )
    suspend fun deleteThreadGateHiddenPosts(
        threadGateUris: Collection<ThreadGateUri>,
    )
}
