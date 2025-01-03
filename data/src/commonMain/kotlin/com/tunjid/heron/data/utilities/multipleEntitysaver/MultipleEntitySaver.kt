package com.tunjid.heron.data.utilities.multipleEntitysaver

import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.NotificationsDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.daos.TimelineDao
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.database.entities.NotificationEntity
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
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
import com.tunjid.heron.data.database.entities.profile.ProfileProfileRelationshipsEntity
import com.tunjid.heron.data.network.models.postExternalEmbedEntity
import com.tunjid.heron.data.network.models.postImageEntity
import com.tunjid.heron.data.network.models.postVideoEntity
import me.tatarka.inject.annotations.Inject

class MultipleEntitySaverProvider @Inject constructor(
    private val postDao: PostDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val notificationsDao: NotificationsDao,
    private val transactionWriter: TransactionWriter,
) {
    internal suspend fun saveInTransaction(
        block: suspend MultipleEntitySaver.() -> Unit,
    ) = MultipleEntitySaver(
        postDao = postDao,
        embedDao = embedDao,
        profileDao = profileDao,
        timelineDao = timelineDao,
        notificationsDao = notificationsDao,
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
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val timelineDao: TimelineDao,
    private val notificationsDao: NotificationsDao,
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

    private val profileProfileRelationshipsEntities =
        mutableListOf<ProfileProfileRelationshipsEntity>()

    private val listEntities =
        mutableListOf<ListEntity>()

    private val feedGeneratorEntities =
        mutableListOf<FeedGeneratorEntity>()

    private val notificationEntities =
        mutableListOf<NotificationEntity>()

    /**
     * Saves all entities added to this [MultipleEntitySaver] in a single transaction
     * and clears the saved models for the next transaction.
     */
    suspend fun saveInTransaction() = transactionWriter.inTransaction {
        // Order matters to satisfy foreign key constraints
        profileDao.insertOrPartiallyUpdateProfiles(profileEntities)

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
        profileDao.upsertProfileProfileRelationships(
            profileProfileRelationshipsEntities
        )

        notificationsDao.upsertNotifications(notificationEntities)

        timelineDao.upsertLists(listEntities)
        timelineDao.upsertFeedGenerators(feedGeneratorEntities)

        timelineDao.upsertTimelineItems(timelineItemEntities)
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

    fun add(entity: ProfileProfileRelationshipsEntity) =
        profileProfileRelationshipsEntities.add(entity)

    fun add(entity: ListEntity) = listEntities.add(entity)

    fun add(entity: FeedGeneratorEntity) = feedGeneratorEntities.add(entity)

    fun add(entity: NotificationEntity) = notificationEntities.add(entity)

    private fun add(entity: ExternalEmbedEntity) = externalEmbedEntities.add(entity)

    private fun add(entity: PostExternalEmbedEntity) = postExternalEmbedEntities.add(entity)

    private fun add(entity: ImageEntity) = imageEntities.add(entity)

    private fun add(entity: PostImageEntity) = postImageEntities.add(entity)

    private fun add(entity: VideoEntity) = videoEntities.add(entity)

    private fun add(entity: PostVideoEntity) = postVideoEntities.add(entity)

}
