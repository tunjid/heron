package com.tunjid.heron.data.utilities.multipleEntitysaver

import app.bsky.graph.ListView
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.ListEntity
import com.tunjid.heron.data.network.models.profileEntity

internal fun MultipleEntitySaver.add(
    listView: ListView
) {
    listView.creator.profileEntity().let(::add)
    add(
        ListEntity(
            cid = listView.cid.cid.let(::Id),
            uri = listView.uri.atUri.let(::Uri),
            creatorId = listView.creator.did.did.let(::Id),
            name = listView.name,
            description = listView.description,
            avatar = listView.avatar?.uri?.let(::Uri),
            listItemCount = listView.listItemCount,
            purpose = listView.purpose.value,
            indexedAt = listView.indexedAt,
        )
    )
}