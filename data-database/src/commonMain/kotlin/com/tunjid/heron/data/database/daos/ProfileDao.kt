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
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.partial
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.database.entities.profile.partial
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query(
        """
            SELECT * FROM profiles
            WHERE did IN (:ids)
            OR handle IN (:ids)
        """
    )
    fun profiles(
        ids: List<Id.Profile>,
    ): Flow<List<ProfileEntity>>

    @Query(
        """
            SELECT * FROM profileViewerStates
            INNER JOIN profiles
            ON profileId = did
            WHERE profileId = :profileId
                AND otherProfileId IN (:otherProfileIds)
            OR handle = :profileId
                AND otherProfileId IN (:otherProfileIds)
        """
    )
    fun viewerState(
        profileId: String,
        otherProfileIds: Set<Id.Profile>,
    ): Flow<List<ProfileViewerStateEntity>>

    @Upsert
    suspend fun upsertProfiles(
        entities: List<ProfileEntity>,
    )

    @Transaction
    suspend fun insertOrPartiallyUpdateProfiles(
        entities: List<ProfileEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = ProfileEntity::partial,
        insertEntities = ::insertOrIgnoreProfiles,
        updatePartials = ::updatePartialProfiles
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreProfiles(
        entities: List<ProfileEntity>,
    ): List<Long>

    @Update(entity = ProfileEntity::class)
    suspend fun updatePartialProfiles(
        entities: List<ProfileEntity.Partial>,
    )

    @Upsert
    suspend fun upsertProfileViewers(
        entities: List<ProfileViewerStateEntity>,
    )

    @Transaction
    suspend fun insertOrPartiallyUpdateProfileViewers(
        entities: List<ProfileViewerStateEntity>,
    ) = partialUpsert(
        items = entities,
        partialMapper = ProfileViewerStateEntity::partial,
        insertEntities = ::insertOrIgnoreProfileViewers,
        updatePartials = ::updatePartialProfileViewers
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreProfileViewers(
        entities: List<ProfileViewerStateEntity>,
    ): List<Long>

    @Update(entity = ProfileViewerStateEntity::class)
    suspend fun updatePartialProfileViewers(
        entities: List<ProfileViewerStateEntity.Partial>,
    )

}