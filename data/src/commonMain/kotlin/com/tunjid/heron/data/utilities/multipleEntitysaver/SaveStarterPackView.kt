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
import com.tunjid.heron.data.core.types.GenericId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.StarterPackEntity
import com.tunjid.heron.data.network.models.profileEntity
import app.bsky.graph.Starterpack as BskyStarterPack

internal fun MultipleEntitySaver.add(
    starterPack: StarterPackView,
) {
    val bskyStarterPack = try {
        starterPack.record.decodeAs<BskyStarterPack>()
    } catch (e: Exception) {
        return
    }
    starterPack.creator.profileEntity().let(::add)
    starterPack.feeds.forEach(::add)
    starterPack.list?.let { listView ->
        add(
            creator = starterPack.creator,
            listView = listView,
        )
        starterPack.listItemsSample.forEach { listItemView ->
            add(
                listUri = listView.uri.atUri.let(::ListUri),
                listItemView = listItemView
            )
        }
    }
    add(
        StarterPackEntity(
            cid = starterPack.cid.cid.let(::GenericId),
            uri = starterPack.uri.atUri.let(::GenericUri),
            creatorId = starterPack.creator.did.did.let(::ProfileId),
            listUri = starterPack.list?.uri?.atUri?.let(::ListUri),
            name = bskyStarterPack.name,
            joinedWeekCount = starterPack.joinedWeekCount,
            joinedAllTimeCount = starterPack.joinedAllTimeCount,
            createdAt = bskyStarterPack.createdAt,
            indexedAt = starterPack.indexedAt,
        )
    )
}
