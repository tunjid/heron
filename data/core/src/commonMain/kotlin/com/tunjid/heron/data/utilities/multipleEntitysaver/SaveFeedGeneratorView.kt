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

import app.bsky.feed.GeneratorView
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.FeedGeneratorEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.tidInstant

internal fun MultipleEntitySaver.add(
    feedGeneratorView: GeneratorView,
) {
    feedGeneratorView.creator.profileEntity().let(::add)
    feedGeneratorView.labels.forEach(::add)
    add(
        FeedGeneratorEntity(
            cid = feedGeneratorView.cid.cid.let(::FeedGeneratorId),
            did = feedGeneratorView.did.did.let(::FeedGeneratorId),
            uri = feedGeneratorView.uri.atUri.let(::FeedGeneratorUri),
            creatorId = feedGeneratorView.creator.did.did.let(::ProfileId),
            displayName = feedGeneratorView.displayName,
            description = feedGeneratorView.description,
            avatar = feedGeneratorView.avatar?.uri?.let(::ImageUri),
            likeCount = feedGeneratorView.likeCount,
            acceptsInteractions = feedGeneratorView.acceptsInteractions,
            contentMode = feedGeneratorView.contentMode,
            indexedAt = feedGeneratorView.indexedAt,
            createdAt = feedGeneratorView.uri.tidInstant ?: feedGeneratorView.indexedAt,
        )
    )
}