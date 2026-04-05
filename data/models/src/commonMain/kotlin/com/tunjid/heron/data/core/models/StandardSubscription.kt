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

import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.StandardPublicationUri
import com.tunjid.heron.data.core.types.StandardSubscriptionId
import com.tunjid.heron.data.core.types.StandardSubscriptionUri
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StandardSubscription(
    val uri: StandardSubscriptionUri,
    val cid: StandardSubscriptionId?,
    val publicationUri: StandardPublicationUri,
    val sortedAt: Instant,
) : Record {
    override val reference: Record.Reference =
        Record.Reference(
            id = cid,
            uri = uri,
        )

    @Serializable
    data class Create(
        val publicationUri: StandardPublicationUri,
        val recordKey: RecordKey = RecordKey.generate(),
        val sortedAt: Instant = Clock.System.now(),
    )
}
