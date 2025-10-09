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

package com.tunjid.heron.data.network.models

import app.bsky.feed.Threadgate as BskyThreadGate
import app.bsky.feed.ThreadgateAllowUnion
import app.bsky.feed.ThreadgateFollowerRule
import app.bsky.feed.ThreadgateFollowingRule
import app.bsky.feed.ThreadgateListRule
import app.bsky.feed.ThreadgateMentionRule
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.utilities.asJsonContent
import kotlin.time.Clock
import sh.christian.ozone.api.AtUri

fun Post.Interaction.Upsert.Gate.toNetworkRecord() = BskyThreadGate(
    createdAt = Clock.System.now(),
    post = postUri.uri.let(::AtUri),
    allow = when {
        // Everyone can reply
        allowedListUris.isEmpty() &&
            allowsFollowers &&
            allowsFollowing &&
            allowsMentioned -> null
        else -> buildList {
            if (allowsFollowers) add(
                ThreadgateAllowUnion.FollowerRule(ThreadgateFollowerRule),
            )
            if (allowsFollowing) add(
                ThreadgateAllowUnion.FollowingRule(ThreadgateFollowingRule),
            )
            if (allowsMentioned) add(
                ThreadgateAllowUnion.MentionRule(ThreadgateMentionRule),
            )
            addAll(
                allowedListUris.map {
                    ThreadgateAllowUnion.ListRule(
                        ThreadgateListRule(it.uri.let(::AtUri)),
                    )
                },
            )
        }
    },
    hiddenReplies = emptyList(),
)
    .asJsonContent(BskyThreadGate.serializer())
