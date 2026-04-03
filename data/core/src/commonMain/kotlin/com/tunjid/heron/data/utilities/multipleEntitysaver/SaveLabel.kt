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

import com.atproto.label.Label
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.LabelEntity
import kotlin.time.Clock

internal fun MultipleEntitySaver.add(
    label: Label,
) {
    val entity = label.toLabelEntity()

    // Negation labels remove previously applied labels
    if (label.neg == true) {
        remove(entity)
        return
    }
    // Skip already-expired labels
    val exp = label.exp
    if (exp != null && exp <= Clock.System.now()) return

    stubProfileEntity(label.src).let(::add)
    add(entity)
}

private fun Label.toLabelEntity() = LabelEntity(
    cid = cid?.cid,
    uri = uri.uri.let(::GenericUri),
    creatorId = src.did.let(::ProfileId),
    value = `val`,
    version = ver,
    createdAt = cts,
    expiresAt = exp,
)
