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

package com.tunjid.heron.data.core.models

import com.tunjid.heron.data.core.types.EmbeddableRecordUri
import com.tunjid.heron.data.core.types.FeedGeneratorId
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.data.core.types.FeedReqId
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FeedGenerator(
    val cid: FeedGeneratorId,
    val did: ProfileId,
    val uri: FeedGeneratorUri,
    val avatar: ImageUri?,
    val likeCount: Long?,
    val creator: Profile,
    val displayName: String,
    val description: String?,
    val contentMode: String?,
    val acceptsInteractions: Boolean?,
    val indexedAt: Instant,
    val labels: List<Label>,
) : UrlEncodableModel,
    Record.Embeddable.Native {

    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )

    override val embeddableRecordUri: EmbeddableRecordUri
        get() = uri

    @Serializable
    sealed class Interaction {
        abstract val feedUri: FeedGeneratorUri
        abstract val postUri: PostUri
        abstract val event: Event
        abstract val feedContext: String?
        abstract val reqId: FeedReqId?

        data class Context(
            val feedUri: FeedGeneratorUri,
            val feedContext: String?,
            val reqId: FeedReqId?,
        )

        @Serializable
        data class Request(
            override val feedUri: FeedGeneratorUri,
            override val postUri: PostUri,
            override val event: Event.Request,
            override val feedContext: String?,
            override val reqId: FeedReqId? = null,
        ) : Interaction()

        @Serializable
        data class Click(
            override val feedUri: FeedGeneratorUri,
            override val postUri: PostUri,
            override val event: Event.Click,
            override val feedContext: String?,
            override val reqId: FeedReqId? = null,
        ) : Interaction()

        @Serializable
        data class Engagement(
            override val feedUri: FeedGeneratorUri,
            override val postUri: PostUri,
            override val event: Event.Engagement,
            override val feedContext: String?,
            override val reqId: FeedReqId? = null,
        ) : Interaction()

        /**
         * Heron mirror of the interaction tokens declared by `app.bsky.feed.defs#interaction`
         * (generated as `app.bsky.feed.Token`), one nested branch per [Interaction] kind. Kept
         * free of any lexicon dependency; the mapping to the wire type lives in the data layer.
         */
        @Serializable
        sealed class Event {

            /**
             * Explicit feedback asking the feed generator to change what it serves. A deliberate
             * user action, worth persisting and retrying — the only branch enqueueable for now.
             */
            @Serializable
            sealed class Request : Event() {

                @Serializable
                data object Less : Request()

                @Serializable
                data object More : Request()
            }

            /** The user clicked through from the feed item. Fire-and-forget telemetry. */
            @Serializable
            sealed class Click : Event() {

                @Serializable
                data object Item : Click()

                @Serializable
                data object Author : Click()

                @Serializable
                data object Reposter : Click()

                @Serializable
                data object Embed : Click()
            }

            /** Passive impressions and engagement mirrored back to the feed. Fire-and-forget. */
            @Serializable
            sealed class Engagement : Event() {

                @Serializable
                data object Seen : Engagement()

                @Serializable
                data object Like : Engagement()

                @Serializable
                data object Repost : Engagement()

                @Serializable
                data object Reply : Engagement()

                @Serializable
                data object Quote : Engagement()

                @Serializable
                data object Share : Engagement()
            }
        }
    }
}
