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
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.ListMemberEntity
import com.tunjid.heron.data.database.entities.PopulatedListMemberEntity
import com.tunjid.heron.data.database.entities.partial
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    @Transaction
    suspend fun insertOrPartiallyUpdateLists(
        entities: List<ListEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = ListEntity::partial,
        insertEntities = ::insertOrIgnoreLists,
        updatePartials = ::updatePartialLists,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreLists(
        entities: List<ListEntity>,
    ): List<Long>

    @Transaction
    @Update(entity = ListEntity::class)
    suspend fun updatePartialLists(
        entities: List<ListEntity.Partial>,
    )

    @Upsert
    suspend fun upsertLists(
        entities: List<ListEntity>,
    )

    @Query(
        """
            SELECT * FROM lists
            WHERE uri = :listUri
        """
    )
    fun list(
        listUri: String,
    ): Flow<ListEntity?>

    @Transaction
    @Query(
        """
            SELECT * FROM listMembers
	        WHERE listUri = :listUri
            ORDER BY createdAt
            DESC
            LIMIT :limit
            OFFSET :offset
        """
    )
    fun listMembers(
        listUri: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedListMemberEntity>>

    @Upsert
    suspend fun upsertListItems(
        entities: List<ListMemberEntity>,
    )
}
