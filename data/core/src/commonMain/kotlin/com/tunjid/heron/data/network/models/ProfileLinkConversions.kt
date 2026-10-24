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

package com.tunjid.heron.data.network.models

import app.bsky.actor.ProfileLinkView
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.ProfileLinkId
import com.tunjid.heron.data.core.types.ProfileLinkUri
import com.tunjid.heron.data.database.entities.profile.ProfileLinkEntity

internal fun ProfileLinkView.profileLinkEntity(
    profileId: ProfileId,
    sortOrder: Int,
): ProfileLinkEntity =
    ProfileLinkEntity(
        uri = uri.atUri.let(::ProfileLinkUri),
        cid = cid.cid.let(::ProfileLinkId),
        profileId = profileId,
        url = url.uri.let(::GenericUri),
        title = title,
        icon = icon?.uri?.let(::ImageUri),
        sortOrder = sortOrder,
    )
