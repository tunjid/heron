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

import app.bsky.feed.GetLikesLike
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.PostLikeEntity

internal fun MultipleEntitySaver.add(
    viewingProfileId: ProfileId?,
    postId: PostId,
    like: GetLikesLike,
) {
    add(
        PostLikeEntity(
            postUri = postId,
            authorId = like.actor.did.did.let(::ProfileId),
            createdAt = like.createdAt,
            indexedAt = like.indexedAt,
        )
    )
    add(
        viewingProfileId = viewingProfileId,
        profileView = like.actor,
    )
}