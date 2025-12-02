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

import app.bsky.graph.StarterPackView
import app.bsky.graph.StarterPackViewBasic
import app.bsky.graph.Starterpack as BskyStarterPack
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.StarterPackId
import com.tunjid.heron.data.core.types.StarterPackUri
import com.tunjid.heron.data.database.entities.StarterPackEntity
import com.tunjid.heron.data.network.models.profileEntity

internal fun MultipleEntitySaver.add(
    starterPack: StarterPackView,
) {
    val bskyStarterPack = try {
        starterPack.record.decodeAs<BskyStarterPack>()
    } catch (_: Exception) {
        return
    }
    starterPack.creator.profileEntity().let(::add)
    starterPack.feeds?.forEach(::add)
    starterPack.labels?.forEach(::add)
    starterPack.list?.let { listView ->
        add(
            listView = listView,
            creator = starterPack.creator::profileEntity,
        )
        starterPack.listItemsSample?.forEach { listItemView ->
            add(
                listUri = listView.uri.atUri.let(::ListUri),
                listItemView = listItemView,
            )
        }
    }
    add(
        StarterPackEntity(
            cid = starterPack.cid.cid.let(::StarterPackId),
            uri = starterPack.uri.atUri.let(::StarterPackUri),
            creatorId = starterPack.creator.did.did.let(::ProfileId),
            listUri = starterPack.list?.uri?.atUri?.let(::ListUri),
            name = bskyStarterPack.name,
            description = bskyStarterPack.description,
            joinedWeekCount = starterPack.joinedWeekCount,
            joinedAllTimeCount = starterPack.joinedAllTimeCount,
            createdAt = bskyStarterPack.createdAt,
            indexedAt = starterPack.indexedAt,
        ),
    )
}

internal fun MultipleEntitySaver.add(
    starterPackView: StarterPackViewBasic,
) {
    val bskyStarterPack = try {
        starterPackView.record.decodeAs<BskyStarterPack>()
    } catch (_: Exception) {
        return
    }
    starterPackView.creator.profileEntity().let(::add)

    add(
        StarterPackEntity(
            cid = starterPackView.cid.cid.let(::StarterPackId),
            uri = starterPackView.uri.atUri.let(::StarterPackUri),
            creatorId = starterPackView.creator.did.did.let(::ProfileId),
            listUri = bskyStarterPack.list.atUri.let(::ListUri),
            name = bskyStarterPack.name,
            description = bskyStarterPack.description,
            joinedWeekCount = starterPackView.joinedWeekCount,
            joinedAllTimeCount = starterPackView.joinedAllTimeCount,
            createdAt = bskyStarterPack.createdAt,
            indexedAt = starterPackView.indexedAt,
        ),
    )
}
