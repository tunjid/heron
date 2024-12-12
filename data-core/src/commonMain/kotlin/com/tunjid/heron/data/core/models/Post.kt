/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant


data class Post(
    val cid: Id,
    val uri: Uri?,
    val author: Profile,
    val replyCount: Long,
    val repostCount: Long,
    val likeCount: Long,
    val quoteCount: Long,
    val indexedAt: Instant,
    val embed: Embed?,
    val quote: Post?,
    val record: Record?,
//    public val viewer: ViewerState? = null,
//    public val labels: List<Label> = emptyList(),
//    public val threadgate: ThreadgateView? = null,
) {
    data class Record(
        val text: String,
        val createdAt: Instant,
        val tags: List<String>,
    )
}

//enum class LinkTarget {
//    UserHandleMention,
//    UserDidMention,
//    ExternalLink,
//    Hashtag;
//}
