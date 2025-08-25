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

import app.bsky.graph.ListItemView
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ListMemberUri
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.ListMemberEntity
import com.tunjid.heron.data.network.models.profileEntity
import com.tunjid.heron.data.utilities.tidInstant

internal fun MultipleEntitySaver.add(
    listUri: ListUri,
    listItemView: ListItemView,
) {
    val createdAt = listItemView.uri.atUri.let(::GenericUri).tidInstant ?: return
    add(listItemView.subject.profileEntity())
    add(
        ListMemberEntity(
            listUri = listUri,
            uri = listItemView.uri.atUri.let(::ListMemberUri),
            subjectId = listItemView.subject.did.did.let(::ProfileId),
            createdAt = createdAt,
        ),
    )
}
