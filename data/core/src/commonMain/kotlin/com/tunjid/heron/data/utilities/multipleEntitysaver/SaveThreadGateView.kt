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

import app.bsky.feed.Threadgate
import app.bsky.feed.ThreadgateAllowUnion
import app.bsky.feed.ThreadgateView
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ThreadGateId
import com.tunjid.heron.data.core.types.ThreadGateUri
import com.tunjid.heron.data.core.types.profileId
import com.tunjid.heron.data.database.entities.ThreadGateEntity
import com.tunjid.heron.data.database.entities.ThreadGateHiddenPostEntity
import com.tunjid.heron.data.database.entities.stubPostEntity
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.safeDecodeAs
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(
    threadGateView: ThreadgateView,
) {
    val threadGateUri = threadGateView.uri?.atUri?.let(::ThreadGateUri) ?: return
    val threadGateId = threadGateView.cid?.cid?.let(::ThreadGateId) ?: return
    val threadGate = threadGateView.record?.safeDecodeAs<Threadgate>() ?: return
    val gatedPostUri = threadGate.post.atUri.let(::PostUri)

    threadGateView.lists.forEach { listViewBasic ->
        add(
            listView = listViewBasic,
            creator = {
                stubProfileEntity(
                    listViewBasic.uri.atUri.let(::ListUri).profileId()
                        .id
                        .let(::Did),
                )
            },
        )
    }
    add(
        ThreadGateEntity(
            cid = threadGateId,
            uri = threadGateUri,
            gatedPostUri = gatedPostUri,
            createdAt = threadGate.createdAt,
            allowed = when {
                threadGate.allow.isEmpty() -> null
                else -> {
                    var allowsFollowing = false
                    var allowsFollowers = false
                    var allowsMentioned = false
                    threadGate.allow.forEach { union ->
                        when (union) {
                            is ThreadgateAllowUnion.FollowerRule -> allowsFollowers = true
                            is ThreadgateAllowUnion.FollowingRule -> allowsFollowing = true
                            // Do nothing. Only save lists in the hydrated response in
                            // ThreadgateView.lists
                            is ThreadgateAllowUnion.ListRule -> Unit
                            is ThreadgateAllowUnion.MentionRule -> allowsMentioned = true
                            is ThreadgateAllowUnion.Unknown -> Unit
                        }
                    }
                    ThreadGateEntity.Allowed(
                        allowsFollowing = allowsFollowing,
                        allowsFollowers = allowsFollowers,
                        allowsMentioned = allowsMentioned,
                    )
                }
            },
        ),
    )

    threadGate.hiddenReplies.forEach { hiddenPostAtUri ->
        val hiddenPostUri = PostUri(hiddenPostAtUri.atUri)
        add(
            stubProfileEntity(
                hiddenPostUri.profileId().id.let(::Did),
            ),
        )
        add(
            stubPostEntity(
                id = Collections.stubbedId(::PostId),
                uri = hiddenPostUri,
                authorId = hiddenPostUri.profileId(),
            ),
        )
        add(
            ThreadGateHiddenPostEntity(
                threadGateUri = threadGateUri,
                hiddenPostUri = hiddenPostAtUri.atUri.let(::PostUri),
            ),
        )
    }
}
