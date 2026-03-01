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
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.database.entities.PopulatedStandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardPublicationEntity
import com.tunjid.heron.data.database.entities.StandardSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StandardSiteDao {

    // --- Publications ---

    @Query(
        """
            SELECT * FROM standardPublications
            WHERE uri IN (:uris)
        """,
    )
    fun publications(
        uris: Collection<StandardPublicationUri>,
    ): Flow<List<StandardPublicationEntity>>

    @Upsert
    suspend fun upsertPublications(
        entities: List<StandardPublicationEntity>,
    )

    @Query(
        """
            DELETE FROM standardPublications
            WHERE uri = :uri
        """,
    )
    suspend fun deletePublication(
        uri: StandardPublicationUri,
    )

    // --- Documents ---

    @Transaction
    @Query(
        """
            SELECT * FROM standardDocuments
            WHERE uri IN (:uris)
        """,
    )
    fun documents(
        uris: Collection<StandardDocumentUri>,
    ): Flow<List<PopulatedStandardDocumentEntity>>

    @Transaction
    @Query(
        """
            SELECT * FROM standardDocuments
            WHERE publicationUri = :publicationUri
            ORDER BY publishedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun publicationDocuments(
        publicationUri: StandardPublicationUri,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedStandardDocumentEntity>>

    @Upsert
    suspend fun upsertDocuments(
        entities: List<StandardDocumentEntity>,
    )

    @Query(
        """
            DELETE FROM standardDocuments
            WHERE uri = :uri
        """,
    )
    suspend fun deleteDocument(
        uri: StandardDocumentUri,
    )

    // --- Subscriptions ---

    @Query(
        """
            SELECT * FROM standardSubscriptions
            WHERE uri IN (:uris)
        """,
    )
    fun subscriptions(
        uris: Collection<StandardSubscriptionUri>,
    ): Flow<List<StandardSubscriptionEntity>>

    @Upsert
    suspend fun upsertSubscriptions(
        entities: List<StandardSubscriptionEntity>,
    )

    @Query(
        """
            DELETE FROM standardSubscriptions
            WHERE uri = :uri
        """,
    )
    suspend fun deleteSubscription(
        uri: StandardSubscriptionUri,
    )
}
