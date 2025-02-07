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
import com.tunjid.heron.data.core.models.UriLookup
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.network.NetworkService
import kotlinx.coroutines.flow.first
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle


internal suspend fun lookupUri(
    networkService: NetworkService,
    profileDao: ProfileDao,
    uriLookup: UriLookup,
): AtUri? {
    val profile = uriLookup.profileHandleOrDid
    val profileDid = when {
        Did.Regex.matches(profile) -> Did(profile)
        Handle.Regex.matches(profile) -> profileDao.profiles(
            ids = listOf(Id(profile))
        )
            .first()
            .takeIf { it.isNotEmpty() }
            ?.first()
            ?.did
            ?.id
            ?.let { Did(it) }
            ?: runCatchingWithNetworkRetry {
                networkService.api.resolveHandle(
                    params = ResolveHandleQueryParams(
                        Handle(profile)
                    )
                )
            }
                .getOrNull()
                ?.did

        else -> null
    }
        ?: return null

    return AtUri(
        when (uriLookup) {
            is UriLookup.Timeline.FeedGenerator -> "at://${profileDid.did}/$FeedGeneratorCollection/${uriLookup.feedUriSuffix}"
            is UriLookup.Timeline.List -> "at://${profileDid.did}/$ListCollection/${uriLookup.listUriSuffix}"
            is UriLookup.Post -> "at://${profileDid.did}/$PostCollection${uriLookup.postUriSuffix}"
            is UriLookup.Profile -> "at://${profileDid.did}"
            is UriLookup.Timeline.Profile -> "at://${profileDid.did}"
        }
    )
}

private const val PostCollection = "app.bsky.feed.post"
private const val ListCollection = "app.bsky.graph.list"
private const val FeedGeneratorCollection = "app.bsky.feed.generator"