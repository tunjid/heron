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
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.PopulatedFeedGeneratorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedGeneratorDao {

    @Transaction
    @Query(
        """
            SELECT * FROM feedGenerators
            WHERE uri IN (:feedUris)
        """
    )
    fun feedGenerators(
        feedUris: Collection<FeedGeneratorUri>
    ): Flow<List<PopulatedFeedGeneratorEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM feedGenerators
	        WHERE creatorId = :creatorId
            ORDER BY indexedAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun profileFeedGenerators(
        creatorId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedFeedGeneratorEntity>>

    @Transaction @Upsert suspend fun upsertFeedGenerators(entities: List<FeedGeneratorEntity>)

    @Query(
        """
            DELETE FROM feedGenerators
            WHERE uri = :uri
        """
    )
    suspend fun deleteFeedGenerator(uri: FeedGeneratorUri)
}
