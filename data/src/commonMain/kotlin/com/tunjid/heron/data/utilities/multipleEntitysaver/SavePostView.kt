package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.feed.PostView
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.network.models.embedEntities
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.network.models.profileProfileRelationshipsEntities
import com.tunjid.heron.data.network.models.quotedPostEmbedEntities
import com.tunjid.heron.data.network.models.quotedPostEntity
import com.tunjid.heron.data.network.models.quotedPostProfileEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: Id,
    postView: PostView,
) {
    val postEntity = postView.postEntity().also(::add)

    add(
        viewingProfileId = viewingProfileId,
        profileView = postView.author,
    )
    postView.embedEntities().forEach { embedEntity ->
        associatePostEmbeds(
            postEntity = postEntity,
            embedEntity = embedEntity,
        )
    }

    postView.viewer?.postViewerStatisticsEntity(
        postId = postEntity.cid,
    )?.let(::add)

    postView.quotedPostEntity()?.let { embeddedPostEntity ->
        add(embeddedPostEntity)
        add(
            PostPostEntity(
                postId = postEntity.cid,
                embeddedPostId = embeddedPostEntity.cid,
            )
        )
        postView.quotedPostEmbedEntities().forEach { embedEntity ->
            associatePostEmbeds(
                postEntity = embeddedPostEntity,
                embedEntity = embedEntity,
            )
        }
    }
    postView.quotedPostProfileEntity()?.let(::add)
}