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
import com.tunjid.heron.data.database.entities.messageembeds.MessageFeedGeneratorEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageListEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessagePostEntity
import com.tunjid.heron.data.database.entities.messageembeds.MessageStarterPackEntity
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
import com.tunjid.heron.data.utilities.LazyList
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
    private val timelineItemEntities = LazyList<TimelineItemEntity>()

    private val postEntities = LazyList<PostEntity>()

    private val profileEntities = LazyList<ProfileEntity>()

    private val postPostEntities = LazyList<PostPostEntity>()

    private val externalEmbedEntities = LazyList<ExternalEmbedEntity>()

    private val postExternalEmbedEntities = LazyList<PostExternalEmbedEntity>()

    private val imageEntities = LazyList<ImageEntity>()

    private val postImageEntities = LazyList<PostImageEntity>()

    private val videoEntities = LazyList<VideoEntity>()

    private val postVideoEntities = LazyList<PostVideoEntity>()

    private val postThreadEntities = LazyList<PostThreadEntity>()

    private val postViewerStatisticsEntities = LazyList<PostViewerStatisticsEntity>()

    private val postLikeEntities = LazyList<PostLikeEntity>()

    private val profileViewerEntities = LazyList<ProfileViewerStateEntity>()

    private val listEntities = LazyList<ListEntity>()

    private val feedGeneratorEntities = LazyList<FeedGeneratorEntity>()

    private val notificationEntities = LazyList<NotificationEntity>()

    private val starterPackEntities = LazyList<StarterPackEntity>()

    private val listItemEntities = LazyList<ListMemberEntity>()

    private val conversationEntities = LazyList<ConversationEntity>()

    private val conversationMemberEntities = LazyList<ConversationMembersEntity>()

    private val messageEntities = LazyList<MessageEntity>()

    private val messageFeedGeneratorEntities = LazyList<MessageFeedGeneratorEntity>()

    private val messageListEntities = LazyList<MessageListEntity>()

    private val messagePostEntities = LazyList<MessagePostEntity>()

    private val messageStarterPackEntities = LazyList<MessageStarterPackEntity>()


    /**
     * Saves all entities added to this [MultipleEntitySaver] in a single transaction
     * and clears the saved models for the next transaction.
     */
    suspend fun saveInTransaction() = transactionWriter.inTransaction {
        // Order matters to satisfy foreign key constraints
        val (fullProfileEntities, partialProfileEntities) = profileEntities.list.partition {
            it.handle != Constants.unknownAuthorHandle
                    && it.followersCount != null
                    && it.followsCount != null
                    && it.postsCount != null
        }
        // Profiles from messages may just be empty profiles with Dids
        val (usablePartialProfileEntities, emptyProfileEntities) = partialProfileEntities.partition {
            it.handle != Constants.unknownAuthorHandle && it.displayName != null
        }
        profileDao.upsertProfiles(fullProfileEntities)
        profileDao.insertOrPartiallyUpdateProfiles(usablePartialProfileEntities)
        profileDao.insertOrIgnoreProfiles(emptyProfileEntities)

        postDao.upsertPosts(postEntities.list)

        embedDao.upsertExternalEmbeds(externalEmbedEntities.list)
        embedDao.upsertImages(imageEntities.list)
        embedDao.upsertVideos(videoEntities.list)

        postDao.insertOrIgnorePostPosts(postPostEntities.list)

        postDao.insertOrIgnorePostExternalEmbeds(postExternalEmbedEntities.list)
        postDao.insertOrIgnorePostImages(postImageEntities.list)
        postDao.insertOrIgnorePostVideos(postVideoEntities.list)

        postDao.upsertPostThreads(postThreadEntities.list)
        postDao.upsertPostStatistics(postViewerStatisticsEntities.list)
        postDao.upsertPostLikes(postLikeEntities.list)

        val (fullProfileViewerEntities, partialProfileViewerEntities) = profileViewerEntities.list
            .partition {
            it.commonFollowersCount != null
        }
        profileDao.upsertProfileViewers(
            fullProfileViewerEntities
        )
        profileDao.insertOrPartiallyUpdateProfileViewers(
            partialProfileViewerEntities
        )

        notificationsDao.upsertNotifications(notificationEntities.list)

        // Order matters to satisfy foreign key constraints
        val (fullListEntities, partialListEntities) = listEntities.list.partition {
            it.description != null && it.indexedAt != Instant.DISTANT_PAST
        }
        listDao.upsertLists(fullListEntities)
        listDao.insertOrPartiallyUpdateLists(partialListEntities)

        listDao.upsertListItems(listItemEntities.list)
        starterPackDao.upsertStarterPacks(starterPackEntities.list)

        feedGeneratorDao.upsertFeedGenerators(feedGeneratorEntities.list)

        timelineDao.upsertTimelineItems(timelineItemEntities.list)

        messageDao.upsertConversations(conversationEntities.list)
        messageDao.upsertConversationMembers(conversationMemberEntities.list)
        messageDao.upsertMessages(messageEntities.list)
        messageDao.upsertMessageFeeds(messageFeedGeneratorEntities.list)
        messageDao.upsertMessageLists(messageListEntities.list)
        messageDao.upsertMessageStarterPacks(messageStarterPackEntities.list)
        messageDao.upsertMessagePosts(messagePostEntities.list)
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

    fun add(entity: MessageFeedGeneratorEntity) = messageFeedGeneratorEntities.add(entity)

    fun add(entity: MessageListEntity) = messageListEntities.add(entity)

    fun add(entity: MessagePostEntity) = messagePostEntities.add(entity)

    fun add(entity: MessageStarterPackEntity) = messageStarterPackEntities.add(entity)

    private fun add(entity: ExternalEmbedEntity) = externalEmbedEntities.add(entity)

    private fun add(entity: PostExternalEmbedEntity) = postExternalEmbedEntities.add(entity)

    private fun add(entity: ImageEntity) = imageEntities.add(entity)

    private fun add(entity: PostImageEntity) = postImageEntities.add(entity)

    private fun add(entity: VideoEntity) = videoEntities.add(entity)

    private fun add(entity: PostVideoEntity) = postVideoEntities.add(entity)

}

