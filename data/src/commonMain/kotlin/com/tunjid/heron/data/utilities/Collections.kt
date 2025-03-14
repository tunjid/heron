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

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.types.Uri
import kotlinx.serialization.KSerializer
import sh.christian.ozone.BlueskyJson
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent

internal object Collections {
    const val Post = "app.bsky.feed.post"
    const val Repost = "app.bsky.feed.repost"
    const val Like = "app.bsky.feed.like"
    const val Follow = "app.bsky.graph.follow"

    fun recordKey(uri: Uri) = RKey(
        rkey = uri.recordKey,
    )
}

val Uri.recordKey get() = uri.split("/").last()

internal fun <T> T.asJsonContent(
    serializer: KSerializer<T>,
): JsonContent = BlueskyJson.decodeFromString(
    BlueskyJson.encodeToString(serializer, this)
)
