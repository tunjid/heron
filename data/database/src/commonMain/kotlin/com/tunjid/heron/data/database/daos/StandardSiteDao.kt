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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tunjid.heron.data.core.types.StandardDocumentUri
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import com.tunjid.heron.data.database.entities.PopulatedStandardDocumentEntity
import com.tunjid.heron.data.database.entities.PopulatedStandardPublicationEntity
import com.tunjid.heron.data.database.entities.StandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardPublicationEntity
import com.tunjid.heron.data.database.entities.StandardSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StandardSiteDao {

    // --- Publications ---

    @Query(
        """
            SELECT
                standardPublications.*,
                standardSubscriptions.uri as subscription_uri,
                standardSubscriptions.cid as subscription_cid,
                standardSubscriptions.publicationUri as subscription_publicationUri,
                standardSubscriptions.sortedAt as subscription_sortedAt,
                standardSubscriptions.viewingProfileId as subscription_viewingProfileId,
                profiles.did as publisher_did,
                profiles.handle as publisher_handle,
                profiles.displayName as publisher_displayName,
                profiles.avatar as publisher_avatar
            FROM standardPublications
            LEFT JOIN standardSubscriptions
                ON standardPublications.uri = standardSubscriptions.publicationUri
                AND standardSubscriptions.viewingProfileId IS :viewingProfileId
            LEFT JOIN profiles
                ON standardPublications.publisherId = profiles.did
            WHERE standardPublications.uri = :publicationUri
        """,
    )
    fun publication(
        viewingProfileId: String?,
        publicationUri: String,
    ): Flow<PopulatedStandardPublicationEntity>

    @Transaction
    @Query(
        """
            SELECT
                standardPublications.*,
                standardSubscriptions.uri as subscription_uri,
                standardSubscriptions.cid as subscription_cid,
                standardSubscriptions.publicationUri as subscription_publicationUri,
                standardSubscriptions.sortedAt as subscription_sortedAt,
                standardSubscriptions.viewingProfileId as subscription_viewingProfileId,
                profiles.did as publisher_did,
                profiles.handle as publisher_handle,
                profiles.displayName as publisher_displayName,
                profiles.avatar as publisher_avatar
            FROM standardPublications
            LEFT JOIN standardSubscriptions
                ON standardPublications.uri = standardSubscriptions.publicationUri
                AND standardSubscriptions.viewingProfileId IS :viewingProfileId
            LEFT JOIN profiles
                ON standardPublications.publisherId = profiles.did
            WHERE standardSubscriptions.viewingProfileId IS :viewingProfileId
            ORDER BY standardSubscriptions.sortedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun subscribedPublications(
        viewingProfileId: String?,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedStandardPublicationEntity>>

    @Upsert
    suspend fun upsertPublications(
        entities: List<StandardPublicationEntity>,
    )

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnorePublications(
        entities: List<StandardPublicationEntity>,
    ): List<Long>

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
            SELECT
                standardDocuments.*,
                standardSubscriptions.uri as subscription_uri,
                standardSubscriptions.cid as subscription_cid,
                standardSubscriptions.publicationUri as subscription_publicationUri,
                standardSubscriptions.sortedAt as subscription_sortedAt,
                standardSubscriptions.viewingProfileId as subscription_viewingProfileId,
                profiles.did as publisher_did,
                profiles.handle as publisher_handle,
                profiles.displayName as publisher_displayName,
                profiles.avatar as publisher_avatar
            FROM standardDocuments
            LEFT JOIN standardSubscriptions
                ON standardDocuments.publicationUri = standardSubscriptions.publicationUri
                AND standardSubscriptions.viewingProfileId IS :viewingProfileId
            LEFT JOIN standardPublications
                ON standardPublications.uri = standardDocuments.publicationUri
            LEFT JOIN profiles
                ON standardPublications.publisherId = profiles.did
            WHERE authorId = :authorId
            ORDER BY publishedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun authorDocuments(
        viewingProfileId: String?,
        authorId: String,
        limit: Long,
        offset: Long,
    ): Flow<List<PopulatedStandardDocumentEntity>>

    @Transaction
    @Query(
        """
            SELECT
                standardDocuments.*,
                standardSubscriptions.uri as subscription_uri,
                standardSubscriptions.cid as subscription_cid,
                standardSubscriptions.publicationUri as subscription_publicationUri,
                standardSubscriptions.sortedAt as subscription_sortedAt,
                standardSubscriptions.viewingProfileId as subscription_viewingProfileId,
                profiles.did as publisher_did,
                profiles.handle as publisher_handle,
                profiles.displayName as publisher_displayName,
                profiles.avatar as publisher_avatar
            FROM standardDocuments
            LEFT JOIN standardSubscriptions
                ON standardDocuments.publicationUri = standardSubscriptions.publicationUri
                AND standardSubscriptions.viewingProfileId IS :viewingProfileId
            LEFT JOIN standardPublications
                ON standardPublications.uri = standardDocuments.publicationUri
            LEFT JOIN profiles
                ON standardPublications.publisherId = profiles.did
            WHERE standardDocuments.publicationUri = :publicationUri
            ORDER BY publishedAt DESC
            LIMIT :limit
            OFFSET :offset
        """,
    )
    fun publicationDocuments(
        viewingProfileId: String?,
        publicationUri: String,
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

    @Query(
        """
            DELETE FROM standardDocuments
            WHERE authorId = :authorId
        """,
    )
    suspend fun deleteDocumentsForAuthor(
        authorId: String,
    )

    @Query(
        """
            DELETE FROM standardDocuments
            WHERE publicationUri = :publicationUri
        """,
    )
    suspend fun deleteDocumentsForPublication(
        publicationUri: String,
    )

    // --- Subscriptions ---

    @Upsert
    suspend fun upsertSubscriptions(
        entities: List<StandardSubscriptionEntity>,
    )

    @Delete(entity = StandardSubscriptionEntity::class)
    suspend fun deleteSubscriptions(
        entities: List<StandardSubscriptionEntity.Deletion>,
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

    @Query(
        """
            DELETE FROM standardSubscriptions
            WHERE viewingProfileId = :viewingProfileId
        """,
    )
    suspend fun deleteSubscriptionsForViewer(
        viewingProfileId: String,
    )
}
