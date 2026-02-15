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

package com.tunjid.heron.data.graze

import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GrazeFeed {
    val recordKey: RecordKey

    @Serializable
    sealed interface Editable : GrazeFeed {
        val filter: Filter.Root
    }

    @Serializable
    data class Pending(
        @SerialName("rkey")
        override val recordKey: RecordKey,
        override val filter: Filter.Root,
    ) : Editable

    @Serializable
    data class Created(
        @SerialName("rkey")
        override val recordKey: RecordKey,
        override val filter: Filter.Root,
        val displayName: String? = null,
        val description: String? = null,
    ) : Editable

    @Serializable
    data class Deleted(
        @SerialName("rkey")
        override val recordKey: RecordKey,
    ) : GrazeFeed

    @Serializable
    sealed interface Update {

        @Serializable
        sealed interface Put : Update {
            val feed: GrazeFeed.Editable
        }

        val recordKey: RecordKey

        @Serializable
        data class Create(
            override val feed: Pending,
        ) : Put {
            override val recordKey: RecordKey
                get() = feed.recordKey
        }

        @Serializable
        data class Edit(
            override val feed: Created,
        ) : Put {
            override val recordKey: RecordKey
                get() = feed.recordKey
        }

        @Serializable
        data class Get(
            override val recordKey: RecordKey,
        ) : Update

        @Serializable
        data class Delete(
            override val recordKey: RecordKey,
        ) : Update
    }
}

val FeedGenerator.isGrazeFeed: Boolean
    get() = did == GrazeDid

val GrazeDid = ProfileId("did:web:api.graze.social")
