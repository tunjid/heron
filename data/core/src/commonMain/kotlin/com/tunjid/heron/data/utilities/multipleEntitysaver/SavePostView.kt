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

import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.feed.ThreadgateView
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.postembeds.PostPostEntity
import com.tunjid.heron.data.network.models.embedEntities
import com.tunjid.heron.data.network.models.postEntity
import com.tunjid.heron.data.network.models.postViewerStatisticsEntity
import com.tunjid.heron.data.network.models.quotedPostEmbedEntities
import com.tunjid.heron.data.network.models.quotedPostEntity
import com.tunjid.heron.data.network.models.quotedPostProfileEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    postView: PostView,
) {
    val postEntity = postView.postEntity().also(::add)
    postView.labels?.forEach(::add)

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
        postUri = postEntity.uri,
        viewingProfileId = viewingProfileId,
    )?.let(::add)

    postView.threadgate?.let(::add)

    postView.quotedPostEntity()?.let { embeddedPostEntity ->
        add(embeddedPostEntity)
        add(
            PostPostEntity(
                postUri = postEntity.uri,
                embeddedPostUri = embeddedPostEntity.uri,
            ),
        )
        postView.quotedPostEmbedEntities().forEach { embedEntity ->
            associatePostEmbeds(
                postEntity = embeddedPostEntity,
                embedEntity = embedEntity,
            )
        }
    }

    val recordUnion = when (val embed = postView.embed) {
        is PostViewEmbedUnion.RecordView -> embed.value.record
        is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record
        else -> null
    }

    when (recordUnion) {
        is RecordViewRecordUnion.FeedGeneratorView ->
            add(recordUnion.value)
        is RecordViewRecordUnion.GraphListView ->
            add(recordUnion.value)
        is RecordViewRecordUnion.GraphStarterPackViewBasic ->
            add(recordUnion.value)
        is RecordViewRecordUnion.LabelerLabelerView ->
            add(
                viewingProfileId = viewingProfileId,
                labeler = recordUnion.value,
            )
        is RecordViewRecordUnion.Unknown,
        is RecordViewRecordUnion.ViewBlocked,
        is RecordViewRecordUnion.ViewDetached,
        is RecordViewRecordUnion.ViewNotFound,
        is RecordViewRecordUnion.ViewRecord,
        null,
        -> Unit
    }
    postView.quotedPostProfileEntity()?.let(::add)
}
