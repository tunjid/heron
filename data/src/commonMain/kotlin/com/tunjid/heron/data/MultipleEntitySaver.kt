package com.tunjid.heron.data

import com.tunjid.heron.data.database.TransactionWriter
import com.tunjid.heron.data.database.daos.EmbedDao
import com.tunjid.heron.data.database.daos.PostDao
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.PostEntity
import com.tunjid.heron.data.database.entities.PostThreadEntity
import com.tunjid.heron.data.database.entities.ProfileEntity
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

/**
 * Utility class for persisting multiple entities in a transaction.
 */
internal class MultipleEntitySaver(
    private val postDao: PostDao,
    private val embedDao: EmbedDao,
    private val profileDao: ProfileDao,
    private val transactionWriter: TransactionWriter,
) {
    private val allLists = mutableListOf<MutableList<out Any>>()

    private val postEntities =
        mutableListOf<PostEntity>().also(allLists::add)

    private val profileEntities =
        mutableListOf<ProfileEntity>().also(allLists::add)

    private val postPostEntities =
        mutableListOf<PostPostEntity>().also(allLists::add)

    private val externalEmbedEntities =
        mutableListOf<ExternalEmbedEntity>().also(allLists::add)

    private val postExternalEmbedEntities =
        mutableListOf<PostExternalEmbedEntity>().also(allLists::add)

    private val imageEntities =
        mutableListOf<ImageEntity>().also(allLists::add)

    private val postImageEntities =
        mutableListOf<PostImageEntity>().also(allLists::add)

    private val videoEntities =
        mutableListOf<VideoEntity>().also(allLists::add)

    private val postVideoEntities =
        mutableListOf<PostVideoEntity>().also(allLists::add)

    private val postThreadEntities =
        mutableListOf<PostThreadEntity>().also(allLists::add)

    private val postViewerStatisticsEntities =
        mutableListOf<PostViewerStatisticsEntity>().also(allLists::add)

    private val profileProfileRelationshipsEntities =
        mutableListOf<ProfileProfileRelationshipsEntity>().also(allLists::add)

    /**
     * Saves all entities added to this [MultipleEntitySaver] in a single transaction
     * and clears the saved models for the next transaction.
     */
    suspend fun saveInTransaction(
        beforeSave: suspend () -> Unit = {},
        afterSave: suspend () -> Unit = {},
    ) = transactionWriter.inTransaction {
        beforeSave()

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

        afterSave()

        allLists.forEach(MutableList<out Any>::clear)
    }

    fun MultipleEntitySaver.associatePostEmbeds(
        postEntity: PostEntity,
        embedEntity: PostEmbed,
    ) {
        when (embedEntity) {
            is ExternalEmbedEntity -> {
                externalEmbedEntities.add(embedEntity)
                postExternalEmbedEntities.add(
                    postEntity.postExternalEmbedEntity(embedEntity)
                )
            }

            is ImageEntity -> {
                imageEntities.add(embedEntity)
                postImageEntities.add(
                    postEntity.postImageEntity(embedEntity)
                )
            }

            is VideoEntity -> {
                videoEntities.add(embedEntity)
                postVideoEntities.add(
                    postEntity.postVideoEntity(embedEntity)
                )
            }
        }
    }

    fun add(entity: PostEntity) = postEntities.add(entity)

    fun add(entity: ProfileEntity) = profileEntities.add(entity)

    fun add(entity: PostPostEntity) = postPostEntities.add(entity)

    fun add(entity: ExternalEmbedEntity) = externalEmbedEntities.add(entity)

    fun add(entity: PostExternalEmbedEntity) = postExternalEmbedEntities.add(entity)

    fun add(entity: ImageEntity) = imageEntities.add(entity)

    fun add(entity: PostImageEntity) = postImageEntities.add(entity)

    fun add(entity: VideoEntity) = videoEntities.add(entity)

    fun add(entity: PostThreadEntity) = postThreadEntities.add(entity)

    fun add(entity: PostViewerStatisticsEntity) = postViewerStatisticsEntities.add(entity)

    fun add(entity: ProfileProfileRelationshipsEntity) =
        profileProfileRelationshipsEntities.add(entity)

}

