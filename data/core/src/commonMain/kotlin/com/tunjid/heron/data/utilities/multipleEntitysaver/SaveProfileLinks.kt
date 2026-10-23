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

import app.bsky.actor.ProfileLinkView
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileLinkId
import com.tunjid.heron.data.core.types.ProfileLinkUri
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.entities.profile.ProfileLinkEntity
import com.tunjid.heron.data.network.models.profileLinkEntity
import com.tunjid.heron.data.network.models.stubProfileEntity
import com.tunjid.heron.data.utilities.Collections

internal fun MultipleEntitySaver.add(
    profileId: ProfileId,
    links: List<ProfileLinkView>,
) {
    add(stubProfileEntity(profileId))
    // Add stub so save logic runs in case links are empty.
    add(stubProfileLinkEntity(profileId = profileId))
    links.forEachIndexed { index, linkView ->
        add(
            linkView.profileLinkEntity(
                profileId = profileId,
                sortOrder = index,
            ),
        )
        linkView.labels?.forEach(::add)
    }
}

private fun stubProfileLinkEntity(
    profileId: ProfileId,
) = ProfileLinkEntity(
    uri = ProfileLinkUri(
        uri = "${Uri.Host.AtProto.prefix}${profileId.id}/${ProfileLinkUri.NAMESPACE}/stub",
    ),
    cid = Collections.stubbedId(::ProfileLinkId),
    profileId = profileId,
    url = GenericUri(Collections.PLACEHOLDER_URL),
    title = null,
    icon = null,
    sortOrder = -1,
)
