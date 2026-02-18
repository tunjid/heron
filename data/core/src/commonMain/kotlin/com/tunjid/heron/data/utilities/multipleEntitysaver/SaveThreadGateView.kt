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
import com.tunjid.heron.data.database.entities.ThreadGateAllowedListEntity
import com.tunjid.heron.data.database.entities.ThreadGateEntity
import com.tunjid.heron.data.database.entities.ThreadGateHiddenPostEntity
import com.tunjid.heron.data.database.entities.stubPostEntity
import com.tunjid.heron.data.utilities.Collections
import com.tunjid.heron.data.utilities.safeDecodeAs
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(threadGateView: ThreadgateView) {
    val threadGateUri = threadGateView.uri?.atUri?.let(::ThreadGateUri) ?: return
    val threadGateId = threadGateView.cid?.cid?.let(::ThreadGateId) ?: return
    val threadGate = threadGateView.record?.safeDecodeAs<Threadgate>() ?: return
    val gatedPostUri = threadGate.post.atUri.let(::PostUri)

    threadGateView.lists?.forEach { listViewBasic ->
        val allowedListUri = listViewBasic.uri.atUri.let(::ListUri)
        add(
            listView = listViewBasic,
            creator = { stubProfileEntity(allowedListUri.profileId().id.let(::Did)) },
        )
        add(
            ThreadGateAllowedListEntity(
                threadGateUri = threadGateUri,
                allowedListUri = allowedListUri,
            )
        )
    }
    add(
        ThreadGateEntity(
            cid = threadGateId,
            uri = threadGateUri,
            gatedPostUri = gatedPostUri,
            createdAt = threadGate.createdAt,
            allowed =
                when (val allow = threadGate.allow) {
                    // All can reply
                    null -> null
                    // None can reply
                    emptyList<ThreadgateAllowUnion>() ->
                        ThreadGateEntity.Allowed(
                            allowsFollowing = false,
                            allowsFollowers = false,
                            allowsMentioned = false,
                        )
                    else ->
                        allow
                            .groupBy { it::class }
                            .let { grouped ->
                                // ThreadgateAllowUnion.ListRule is handled with the list views
                                // above
                                ThreadGateEntity.Allowed(
                                    allowsFollowing =
                                        ThreadgateAllowUnion.FollowingRule::class in grouped,
                                    allowsFollowers =
                                        ThreadgateAllowUnion.FollowerRule::class in grouped,
                                    allowsMentioned =
                                        ThreadgateAllowUnion.MentionRule::class in grouped,
                                )
                            }
                },
        )
    )

    threadGate.hiddenReplies?.forEach { hiddenPostAtUri ->
        val hiddenPostUri = PostUri(hiddenPostAtUri.atUri)
        val hiddenPostAuthorId = hiddenPostUri.profileId()
        add(stubProfileEntity(hiddenPostAuthorId.id.let(::Did)))
        add(
            stubPostEntity(
                id = Collections.stubbedId(::PostId),
                uri = hiddenPostUri,
                authorId = hiddenPostAuthorId,
            )
        )
        add(
            ThreadGateHiddenPostEntity(threadGateUri = threadGateUri, hiddenPostUri = hiddenPostUri)
        )
    }
}
