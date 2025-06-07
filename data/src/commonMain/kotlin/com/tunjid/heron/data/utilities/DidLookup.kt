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

package com.tunjid.heron.data.utilities

import com.atproto.identity.ResolveHandleQueryParams
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.ProfileHandleOrId
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.database.entities.ProfileEntity
import com.tunjid.heron.data.network.NetworkService
import kotlinx.coroutines.flow.first
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle

internal suspend fun lookupProfileDid(
    profileId: Id.Profile,
    profileDao: ProfileDao,
    networkService: NetworkService,
): Did? {
    val profileHandleOrId = profileId.id
    return when {
        Did.Regex.matches(profileHandleOrId) -> Did(profileHandleOrId)
        Handle.Regex.matches(profileHandleOrId) -> profileDao.profiles(
            ids = listOf(ProfileHandleOrId(profileHandleOrId))
        )
            .first()
            .takeIf(List<ProfileEntity>::isNotEmpty)
            ?.first()
            ?.did
            ?.id
            ?.let(::Did)
            ?: runCatchingWithNetworkRetry {
                networkService.api.resolveHandle(
                    params = ResolveHandleQueryParams(
                        Handle(profileHandleOrId)
                    )
                )
            }
                .getOrNull()
                ?.did

        else -> null
    }
}