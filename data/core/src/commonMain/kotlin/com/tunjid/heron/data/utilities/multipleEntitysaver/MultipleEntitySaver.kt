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
import com.tunjid.heron.data.core.models.Constants.isUnknown
import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.FeedGeneratorDao
import com.tunjid.heron.data.database.daos.LabelDao
import com.tunjid.heron.data.database.daos.ListDao
import com.tunjid.heron.data.database.daos.MessageDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.StandardSiteDao
import com.tunjid.heron.data.database.daos.StarterPackDao
import com.tunjid.heron.data.database.daos.ThreadGateDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.BookmarkEntity
import com.tunjid.heron.data.database.entities.ConversationEntity
import com.tunjid.heron.data.database.entities.ConversationMembersEntity
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.LabelDefinitionEntity
import com.tunjid.heron.data.database.entities.LabelEntity
import com.tunjid.heron.data.database.entities.LabelerEntity
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.ListMemberEntity
import com.tunjid.heron.data.database.entities.MessageEntity
import com.tunjid.heron.data.database.entities.MessageReactionEntity
import com.tunjid.heron.data.database.entities.NotificationEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostLikeEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.database.entities.StandardDocumentEntity
import com.tunjid.heron.data.database.entities.StandardPublicationEntity
import com.tunjid.heron.data.database.entities.StandardSubscriptionEntity
import com.tunjid.heron.data.database.entities.StarterPackEntity
import com.tunjid.heron.data.database.entities.ThreadGateAllowedListEntity
import com.tunjid.heron.data.database.entities.ThreadGateEntity
import com.tunjid.heron.data.database.entities.ThreadGateHiddenPostEntity
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
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.Collections.isStubbedId
import com.tunjid.heron.data.utilities.LazyList
import com.tunjid.heron.data.utilities.triage
import dev.zacsweers.metro.Inject
import kotlin.time.Instant

class MultipleEntitySaverProvider @Inject constructor(
    private val postDao: PostDao,
    private val labelDao: LabelDao,
    private val listDao: ListDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val notificationsDao: NotificationsDao,
    private val starterPackDao: StarterPackDao,
    private val messageDao: MessageDao,
    private val threadGateDao: ThreadGateDao,
    private val standardSiteDao: StandardSiteDao,
    private val transactionWriter: TransactionWriter,
) {
    internal suspend fun saveInTransaction(
        block: suspend MultipleEntitySaver.() -> Unit,
    ) = MultipleEntitySaver(
        postDao = postDao,
        labelDao = labelDao,
        listDao = listDao,
        embedDao = embedDao,
        profileDao = profileDao,
        timelineDao = timelineDao,
        feedGeneratorDao = feedGeneratorDao,
        notificationsDao = notificationsDao,
        starterPackDao = starterPackDao,
        messageDao = messageDao,
        threadGateDao = threadGateDao,
        standardSiteDao = standardSiteDao,
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
    private val labelDao: LabelDao,
    private val listDao: ListDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val feedGeneratorDao: FeedGeneratorDao,
    private val notificationsDao: NotificationsDao,
    private val starterPackDao: StarterPackDao,
    private val messageDao: MessageDao,
    private val threadGateDao: ThreadGateDao,
    private val standardSiteDao: StandardSiteDao,
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
    private val postBookmarkEntities = LazyList<BookmarkEntity>()

    private val videoEntities = LazyList<VideoEntity>()

    private val postVideoEntities = LazyList<PostVideoEntity>()

    private val postThreadEntities = LazyList<PostThreadEntity>()

    private val postViewerStatisticsEntities = LazyList<PostViewerStatisticsEntity>()

    private val postLikeEntities = LazyList<PostLikeEntity>()

    private val profileViewerEntities = LazyList<ProfileViewerStateEntity>()

    private val listEntities = LazyList<ListEntity>()

    private val labelerEntities = LazyList<LabelerEntity>()
    private val labelDefinitionsEntities = LazyList<LabelDefinitionEntity>()
    private val labelEntities = LazyList<LabelEntity>()
    private val labelEntitiesToDelete = LazyList<LabelEntity>()

    private val feedGeneratorEntities = LazyList<FeedGeneratorEntity>()

    private val notificationEntities = LazyList<NotificationEntity>()

    private val starterPackEntities = LazyList<StarterPackEntity>()

    private val listItemEntities = LazyList<ListMemberEntity>()

    private val conversationEntities = LazyList<ConversationEntity>()

    private val conversationMemberEntities = LazyList<ConversationMembersEntity>()

    private val messageEntities = LazyList<MessageEntity>()

    private val messageReactionEntities = LazyList<MessageReactionEntity>()

    private val messageFeedGeneratorEntities = LazyList<MessageFeedGeneratorEntity>()

    private val messageListEntities = LazyList<MessageListEntity>()

    private val messagePostEntities = LazyList<MessagePostEntity>()

    private val messageStarterPackEntities = LazyList<MessageStarterPackEntity>()

    private val threadGateEntities = LazyList<ThreadGateEntity>()
    private val threadGateAllowedListEntities = LazyList<ThreadGateAllowedListEntity>()
    private val threadGateHiddenPostEntities = LazyList<ThreadGateHiddenPostEntity>()

    private val standardPublicationEntities = LazyList<StandardPublicationEntity>()
    private val standardDocumentEntities = LazyList<StandardDocumentEntity>()
    private val standardSubscriptionEntities = LazyList<StandardSubscriptionEntity>()
    private val standardSubscriptionDeletions = LazyList<StandardSubscriptionEntity.Deletion>()

    /**
     * Saves all entities added to this [MultipleEntitySaver] in a single transaction
     * and clears the saved models for the next transaction.
     */
    suspend fun saveInTransaction() = transactionWriter.inTransaction {
        // Order matters to satisfy foreign key constraints
        if (profileEntities.isNotEmpty) {
            val (fullProfileEntities, usablePartialProfileEntities, emptyProfileEntities) = profileEntities.list.triage(
                firstPredicate = {
                    !it.handle.isUnknown() &&
                        it.followersCount != null &&
                        it.followsCount != null &&
                        it.postsCount != null
                },
                // Profiles from messages may just be empty profiles with Dids
                secondPredicate = {
                    !it.handle.isUnknown() && it.displayName != null
                },
            )
            profileDao.upsertProfiles(fullProfileEntities)
            profileDao.insertOrPartiallyUpdateProfiles(usablePartialProfileEntities)
            profileDao.insertOrIgnoreProfiles(emptyProfileEntities)
        }

        if (postEntities.isNotEmpty) {
            val (fullPostEntities, partialPostEntities, stubbedPostEntities) = postEntities.list.triage(
                firstPredicate = { it.hasThreadGate != null && !it.cid.isStubbedId() },
                secondPredicate = { !it.cid.isStubbedId() },
            )
            postDao.upsertPosts(fullPostEntities)
            postDao.insertOrPartiallyUpdatePosts(partialPostEntities)
            postDao.insertOrIgnorePosts(stubbedPostEntities)
        }

        if (externalEmbedEntities.isNotEmpty) {
            embedDao.upsertExternalEmbeds(externalEmbedEntities.list)
        }
        if (imageEntities.isNotEmpty) {
            embedDao.upsertImages(imageEntities.list)
        }
        if (videoEntities.isNotEmpty) {
            embedDao.upsertVideos(videoEntities.list)
        }

        if (postPostEntities.isNotEmpty) {
            postDao.insertOrIgnorePostPosts(postPostEntities.list)
        }

        if (postExternalEmbedEntities.isNotEmpty) {
            postDao.insertOrIgnorePostExternalEmbeds(postExternalEmbedEntities.list)
        }
        if (postImageEntities.isNotEmpty) {
            postDao.insertOrIgnorePostImages(postImageEntities.list)
        }
        if (postVideoEntities.isNotEmpty) {
            postDao.insertOrIgnorePostVideos(postVideoEntities.list)
        }

        if (postThreadEntities.isNotEmpty) {
            postDao.upsertPostThreads(postThreadEntities.list)
        }
        if (postViewerStatisticsEntities.isNotEmpty) {
            postDao.upsertPostStatistics(postViewerStatisticsEntities.list)
        }
        if (postLikeEntities.isNotEmpty) {
            postDao.upsertPostLikes(postLikeEntities.list)
        }
        if (postBookmarkEntities.isNotEmpty) {
            postDao.upsertBookmarks(postBookmarkEntities.list)
        }

        if (profileViewerEntities.isNotEmpty) {
            val (fullProfileViewerEntities, partialProfileViewerEntities) = profileViewerEntities.list
                .partition {
                    it.commonFollowersCount != null
                }
            profileDao.upsertProfileViewers(
                fullProfileViewerEntities,
            )
            profileDao.insertOrPartiallyUpdateProfileViewers(
                partialProfileViewerEntities,
            )
        }

        if (notificationEntities.isNotEmpty) {
            notificationsDao.upsertNotifications(notificationEntities.list)
        }

        // Order matters to satisfy foreign key constraints
        if (listEntities.isNotEmpty) {
            val (fullListEntities, partialListEntities) = listEntities.list.partition {
                it.description != null && it.indexedAt != Instant.DISTANT_PAST
            }
            listDao.upsertLists(fullListEntities)
            listDao.insertOrPartiallyUpdateLists(partialListEntities)
        }

        if (listItemEntities.isNotEmpty) {
            listDao.upsertListItems(listItemEntities.list)
        }
        if (starterPackEntities.isNotEmpty) {
            starterPackDao.upsertStarterPacks(starterPackEntities.list)
        }

        if (feedGeneratorEntities.isNotEmpty) {
            feedGeneratorDao.upsertFeedGenerators(feedGeneratorEntities.list)
        }

        if (labelerEntities.isNotEmpty) {
            labelDao.upsertLabelers(labelerEntities.list)
        }
        if (labelDefinitionsEntities.isNotEmpty) {
            labelDao.upsertLabelValueDefinitions(labelDefinitionsEntities.list)
        }
        if (labelEntities.isNotEmpty) {
            labelDao.upsertLabels(labelEntities.list)
        }
        if (labelEntitiesToDelete.isNotEmpty) {
            labelDao.deleteLabels(labelEntitiesToDelete.list)
        }

        if (timelineItemEntities.isNotEmpty) {
            timelineDao.insertOrPartiallyUpdateTimelineItems(timelineItemEntities.list)
        }

        if (conversationEntities.isNotEmpty) {
            messageDao.upsertConversations(conversationEntities.list)
        }
        if (conversationMemberEntities.isNotEmpty) {
            messageDao.upsertConversationMembers(conversationMemberEntities.list)
        }
        if (messageEntities.isNotEmpty) {
            messageDao.upsertMessages(messageEntities.list)
            messageDao.deleteMessageReactions(
                messageEntities.list.mapTo(
                    mutableSetOf(),
                    MessageEntity::id,
                ),
            )
        }
        if (messageReactionEntities.isNotEmpty) {
            messageDao.upsertMessageReactions(messageReactionEntities.list)
        }
        if (messageFeedGeneratorEntities.isNotEmpty) {
            messageDao.upsertMessageFeeds(messageFeedGeneratorEntities.list)
        }
        if (messageListEntities.isNotEmpty) {
            messageDao.upsertMessageLists(messageListEntities.list)
        }
        if (messageStarterPackEntities.isNotEmpty) {
            messageDao.upsertMessageStarterPacks(messageStarterPackEntities.list)
        }
        if (messagePostEntities.isNotEmpty) {
            messageDao.upsertMessagePosts(messagePostEntities.list)
        }

        if (postEntities.isNotEmpty) {
            threadGateDao.deleteThreadGates(
                postUris = postEntities.list.mapNotNull {
                    if (it.hasThreadGate == false) it.uri
                    else null
                },
            )
        }

        // Standard site entities: publications before documents/subscriptions (FK ordering)
        if (standardPublicationEntities.isNotEmpty) {
            val (fullPublicationEntities, partialPublicationEntities) = standardPublicationEntities.list.partition {
                it.url != Collections.PLACEHOLDER_URL && it.cid != null
            }
            standardSiteDao.insertOrIgnorePublications(partialPublicationEntities)
            standardSiteDao.upsertPublications(fullPublicationEntities)
        }
        if (standardDocumentEntities.isNotEmpty) {
            standardSiteDao.upsertDocuments(standardDocumentEntities.list)
        }
        if (standardSubscriptionEntities.isNotEmpty) {
            standardSiteDao.upsertSubscriptions(standardSubscriptionEntities.list)
        }
        if (standardSubscriptionDeletions.isNotEmpty) {
            standardSiteDao.deleteSubscriptions(standardSubscriptionDeletions.list)
        }

        if (threadGateEntities.isNotEmpty) {
            threadGateDao.upsertThreadGates(threadGateEntities.list)
            val threadGateUris = threadGateEntities.list.map(
                ThreadGateEntity::uri,
            )
            // keep thread gates in sync
            threadGateDao.deleteThreadGateAllowedLists(threadGateUris)
            threadGateDao.deleteThreadGateHiddenPosts(threadGateUris)
            threadGateDao.upsertThreadGateAllowedLists(threadGateAllowedListEntities.list)
            threadGateDao.upsertThreadGateHiddenPosts(threadGateHiddenPostEntities.list)
        }
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

    fun add(entity: BookmarkEntity) = postBookmarkEntities.add(entity)

    fun add(entity: PostLikeEntity) = postLikeEntities.add(entity)

    fun add(entity: ProfileViewerStateEntity) = profileViewerEntities.add(entity)

    fun add(entity: LabelEntity) = labelEntities.add(entity)

    fun remove(entity: LabelEntity) = labelEntitiesToDelete.add(entity)

    fun add(entity: LabelerEntity) = labelerEntities.add(entity)

    fun add(entity: LabelDefinitionEntity) = labelDefinitionsEntities.add(entity)

    fun add(entity: ListEntity) = listEntities.add(entity)

    fun add(entity: FeedGeneratorEntity) = feedGeneratorEntities.add(entity)

    fun add(entity: NotificationEntity) = notificationEntities.add(entity)

    fun add(entity: StarterPackEntity) = starterPackEntities.add(entity)

    fun add(entity: ListMemberEntity) = listItemEntities.add(entity)

    fun add(entity: ConversationEntity) = conversationEntities.add(entity)

    fun add(entity: ConversationMembersEntity) = conversationMemberEntities.add(entity)

    fun add(entity: MessageEntity) = messageEntities.add(entity)

    fun add(entity: MessageReactionEntity) = messageReactionEntities.add(entity)

    fun add(entity: MessageFeedGeneratorEntity) = messageFeedGeneratorEntities.add(entity)

    fun add(entity: MessageListEntity) = messageListEntities.add(entity)

    fun add(entity: MessagePostEntity) = messagePostEntities.add(entity)

    fun add(entity: MessageStarterPackEntity) = messageStarterPackEntities.add(entity)

    fun add(entity: ThreadGateEntity) = threadGateEntities.add(entity)

    fun add(entity: ThreadGateAllowedListEntity) = threadGateAllowedListEntities.add(entity)

    fun add(entity: ThreadGateHiddenPostEntity) = threadGateHiddenPostEntities.add(entity)

    fun add(entity: StandardPublicationEntity) = standardPublicationEntities.add(entity)

    fun add(entity: StandardDocumentEntity) = standardDocumentEntities.add(entity)

    fun add(entity: StandardSubscriptionEntity) = standardSubscriptionEntities.add(entity)

    fun remove(entity: StandardSubscriptionEntity.Deletion) = standardSubscriptionDeletions.add(entity)

    private fun add(entity: ExternalEmbedEntity) = externalEmbedEntities.add(entity)

    private fun add(entity: PostExternalEmbedEntity) = postExternalEmbedEntities.add(entity)

    private fun add(entity: ImageEntity) = imageEntities.add(entity)

    private fun add(entity: PostImageEntity) = postImageEntities.add(entity)

    private fun add(entity: VideoEntity) = videoEntities.add(entity)

    private fun add(entity: PostVideoEntity) = postVideoEntities.add(entity)
}
