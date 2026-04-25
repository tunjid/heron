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

import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.database.entities.profile.ProfileTabsEntity
import com.tunjid.heron.data.database.entities.profile.plus
import dev.tunjid.heron.actor.GetTabsResponseItemUnion
import sh.christian.ozone.api.Did

internal fun MultipleEntitySaver.add(
    profileId: ProfileId,
    tabs: List<GetTabsResponseItemUnion>,
) {
    add(stubProfileEntity(Did(profileId.id)))
    add(
        tabs.fold(
            ProfileTabsEntity(
                profileId = ProfileId(profileId.id),
                delimitedTabs = "",
            ),
        ) { entity, item ->
            when (item) {
                is GetTabsResponseItemUnion.CollectionTab -> entity + item.value.collection.value
                is GetTabsResponseItemUnion.ProfileTab -> entity + item.value.kind.value
                is GetTabsResponseItemUnion.Unknown -> entity
            }
        },
    )
}
