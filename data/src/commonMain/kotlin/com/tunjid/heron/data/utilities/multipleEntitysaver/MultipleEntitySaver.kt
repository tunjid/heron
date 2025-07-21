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

package com.tunjid.heron.data.utilities.multipleEntitysaver

import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.MessageDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.ConversationEntity
import com.tunjid.heron.data.database.entities.ConversationMembersEntity
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.ListMemberEntity
import com.tunjid.heron.data.database.entities.MessageEntity
import com.tunjid.heron.data.database.entities.NotificationEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostLikeEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.StarterPackEntity
import com.tunjid.heron.data.database.entities.TimelineItemEntity
import com.tunjid.heron.data.database.entities.postembeds.ExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.ImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostEmbed
import com.tunjid.heron.data.database.entities.postembeds.PostExternalEmbedEntity
import com.tunjid.heron.data.database.entities.postembeds.PostImageEntity
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.database.entities.postembeds.PostVideoEntity
import com.tunjid.heron.data.database.entities.postembeds.VideoEntity
import com.tunjid.heron.data.database.entities.profile.PostViewerStatisticsEntity
import com.tunjid.heron.data.database.entities.profile.ProfileViewerStateEntity
import com.tunjid.heron.data.network.models.postExternalEmbedEntity
import com.tunjid.heron.data.network.models.postImageEntity
import com.tunjid.heron.data.network.models.postVideoEntity
import dev.zacsweers.metro.Inject
import kotlinx.datetime.Instant

class MultipleEntitySaverProvider @Inject constructor(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val notificationsDao: NotificationsDao,
    private val starterPackDao: StarterPackDao,
    private val messageDao: MessageDao,
    private val transactionWriter: TransactionWriter,
) {
    internal suspend fun saveInTransaction(
        block: suspend MultipleEntitySaver.() -> Unit,
    ) = MultipleEntitySaver(
        postDao = postDao,
        listDao = listDao,
        embedDao = embedDao,
        profileDao = profileDao,
        timelineDao = timelineDao,
        feedGeneratorDao = feedGeneratorDao,
        notificationsDao = notificationsDao,
        starterPackDao = starterPackDao,
        messageDao = messageDao,
        transactionWriter = transactionWriter,
    ).apply {
        block()
        saveInTransaction()
    }
}

/**
 * Utility class for persisting multiple entities in a transaction.
 */
internal class MultipleEntitySaver(
    private val postDao: PostDao,
    private val listDao: ListDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val notificationsDao: NotificationsDao,
    private val starterPackDao: StarterPackDao,
    private val messageDao: MessageDao,
    private val transactionWriter: TransactionWriter,
) {
    private val timelineItemEntities = mutableListOf<TimelineItemEntity>()

    private val postEntities =
        mutableListOf<PostEntity>()

    private val profileEntities =
        mutableListOf<ProfileEntity>()

    private val postPostEntities =
        mutableListOf<PostPostEntity>()

    private val externalEmbedEntities =
        mutableListOf<ExternalEmbedEntity>()

    private val postExternalEmbedEntities =
        mutableListOf<PostExternalEmbedEntity>()

    private val imageEntities =
        mutableListOf<ImageEntity>()

    private val postImageEntities =
        mutableListOf<PostImageEntity>()

    private val videoEntities =
        mutableListOf<VideoEntity>()

    private val postVideoEntities =
        mutableListOf<PostVideoEntity>()

    private val postThreadEntities =
        mutableListOf<PostThreadEntity>()

    private val postViewerStatisticsEntities =
        mutableListOf<PostViewerStatisticsEntity>()

    private val postLikeEntities =
        mutableListOf<PostLikeEntity>()

    private val profileViewerEntities =
        mutableListOf<ProfileViewerStateEntity>()

    private val listEntities =
        mutableListOf<ListEntity>()

    private val feedGeneratorEntities =
        mutableListOf<FeedGeneratorEntity>()

    private val notificationEntities =
        mutableListOf<NotificationEntity>()

    private val starterPackEntities =
        mutableListOf<StarterPackEntity>()

    private val listItemEntities =
        mutableListOf<ListMemberEntity>()

    private val conversationEntities =
        mutableListOf<ConversationEntity>()

    private val conversationMemberEntities =
        mutableListOf<ConversationMembersEntity>()

    private val messageEntities =
        mutableListOf<MessageEntity>()

    /**
     * Saves all entities added to this [MultipleEntitySaver] in a single transaction
     * and clears the saved models for the next transaction.
     */
    suspend fun saveInTransaction() = transactionWriter.inTransaction {
        // Order matters to satisfy foreign key constraints
        val (fullProfileEntities, partialProfileEntities) = profileEntities.partition {
            it.followersCount != 0L && it.followsCount != 0L && it.postsCount != 0L
        }
        // Profiles from messages may just be empty profiles with Dids
        val (usablePartialProfileEntities, emptyProfileEntities) = partialProfileEntities.partition {
            it.handle != Constants.unknownAuthorHandle
        }
        profileDao.upsertProfiles(fullProfileEntities)
        profileDao.insertOrPartiallyUpdateProfiles(usablePartialProfileEntities)
        profileDao.insertOrIgnoreProfiles(emptyProfileEntities)

        postDao.upsertPosts(postEntities)

        embedDao.upsertExternalEmbeds(externalEmbedEntities)
        embedDao.upsertImages(imageEntities)
        embedDao.upsertVideos(videoEntities)

        postDao.insertOrIgnorePostPosts(postPostEntities)

        postDao.insertOrIgnorePostExternalEmbeds(postExternalEmbedEntities)
        postDao.insertOrIgnorePostImages(postImageEntities)
        postDao.insertOrIgnorePostVideos(postVideoEntities)

        postDao.upsertPostThreads(postThreadEntities)
        postDao.upsertPostStatistics(postViewerStatisticsEntities)
        postDao.upsertPostLikes(postLikeEntities)

        val (fullProfileViewerEntities, partialProfileViewerEntities) = profileViewerEntities.partition {
            it.commonFollowersCount != null
        }
        profileDao.upsertProfileViewers(
            fullProfileViewerEntities
        )
        profileDao.insertOrPartiallyUpdateProfileViewers(
            partialProfileViewerEntities
        )

        notificationsDao.upsertNotifications(notificationEntities)

        // Order matters to satisfy foreign key constraints
        val (fullListEntities, partialListEntities) = listEntities.partition {
            it.description != null && it.indexedAt != Instant.DISTANT_PAST
        }
        listDao.upsertLists(fullListEntities)
        listDao.insertOrPartiallyUpdateLists(partialListEntities)

        listDao.upsertListItems(listItemEntities)
        starterPackDao.upsertStarterPacks(starterPackEntities)

        feedGeneratorDao.upsertFeedGenerators(feedGeneratorEntities)

        timelineDao.upsertTimelineItems(timelineItemEntities)

        messageDao.upsertConversations(conversationEntities)
        messageDao.upsertConversationMembers(conversationMemberEntities)
        messageDao.upsertMessages(messageEntities)
    }

    fun MultipleEntitySaver.associatePostEmbeds(
        postEntity: PostEntity,
        embedEntity: PostEmbed,
    ) {
        when (embedEntity) {
            is ExternalEmbedEntity -> {
                add(embedEntity)
                add(postEntity.postExternalEmbedEntity(embedEntity))
            }

            is ImageEntity -> {
                add(embedEntity)
                add(postEntity.postImageEntity(embedEntity))
            }

            is VideoEntity -> {
                add(embedEntity)
                add(postEntity.postVideoEntity(embedEntity))
            }
        }
    }

    fun add(entity: TimelineItemEntity) = timelineItemEntities.add(entity)

    fun add(entity: PostEntity) = postEntities.add(entity)

    fun add(entity: ProfileEntity) = profileEntities.add(entity)

    fun add(entity: PostPostEntity) = postPostEntities.add(entity)

    fun add(entity: PostThreadEntity) = postThreadEntities.add(entity)

    fun add(entity: PostViewerStatisticsEntity) = postViewerStatisticsEntities.add(entity)

    fun add(entity: PostLikeEntity) = postLikeEntities.add(entity)

    fun add(entity: ProfileViewerStateEntity) =
        profileViewerEntities.add(entity)

    fun add(entity: ListEntity) = listEntities.add(entity)

    fun add(entity: FeedGeneratorEntity) = feedGeneratorEntities.add(entity)

    fun add(entity: NotificationEntity) = notificationEntities.add(entity)

    fun add(entity: StarterPackEntity) = starterPackEntities.add(entity)

    fun add(entity: ListMemberEntity) = listItemEntities.add(entity)

    fun add(entity: ConversationEntity) = conversationEntities.add(entity)

    fun add(entity: ConversationMembersEntity) = conversationMemberEntities.add(entity)

    fun add(entity: MessageEntity) = messageEntities.add(entity)

    private fun add(entity: ExternalEmbedEntity) = externalEmbedEntities.add(entity)

    private fun add(entity: PostExternalEmbedEntity) = postExternalEmbedEntities.add(entity)

    private fun add(entity: ImageEntity) = imageEntities.add(entity)

    private fun add(entity: PostImageEntity) = postImageEntities.add(entity)

    private fun add(entity: VideoEntity) = videoEntities.add(entity)

    private fun add(entity: PostVideoEntity) = postVideoEntities.add(entity)

}
