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

import app.bsky.actor.ProfileViewBasic
import app.bsky.graph.ListView
import app.bsky.graph.ListViewBasic
import com.atproto.label.Label
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ListId
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.tidInstant
import kotlinx.datetime.Instant

internal fun MultipleEntitySaver.add(
    listView: ListView,
) {
    listView.creator.profileEntity().let(::add)
    listView.labels.forEach(::add)
    add(
        ListEntity(
            cid = listView.cid.cid.let(::ListId),
            uri = listView.uri.atUri.let(::ListUri),
            creatorId = listView.creator.did.did.let(::ProfileId),
            name = listView.name,
            description = listView.description,
            avatar = listView.avatar?.uri?.let(::ImageUri),
            listItemCount = listView.listItemCount,
            purpose = listView.purpose.value,
            indexedAt = listView.indexedAt,
            createdAt = listView.uri.tidInstant ?: listView.indexedAt,
        ),
    )
}

internal fun MultipleEntitySaver.add(
    creator: ProfileViewBasic,
    listView: ListViewBasic,
) {
    creator.profileEntity().let(::add)
    add(
        ListEntity(
            cid = listView.cid.cid.let(::ListId),
            uri = listView.uri.atUri.let(::ListUri),
            creatorId = creator.did.did.let(::ProfileId),
            name = listView.name,
            description = "",
            avatar = listView.avatar?.uri?.let(::ImageUri),
            listItemCount = listView.listItemCount,
            purpose = listView.purpose.value,
            indexedAt = listView.indexedAt ?: Instant.DISTANT_PAST,
            createdAt = listView.uri.tidInstant ?: listView.indexedAt ?: Instant.DISTANT_PAST,
        ),
    )
}
