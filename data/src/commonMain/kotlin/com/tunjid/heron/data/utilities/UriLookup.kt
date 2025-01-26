package com.tunjid.heron.data.utilities

import com.atproto.identity.ResolveHandleQueryParams
import com.tunjid.heron.data.core.models.UriLookup
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.database.daos.ProfileDao
import com.tunjid.heron.data.network.NetworkService
import kotlinx.coroutines.flow.first
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Handle


internal suspend fun lookupUri(
    networkService: NetworkService,
    profileDao: ProfileDao,
    uriLookup: UriLookup,
): Uri? {
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
            }.getOrNull()
                ?.did

        else -> null
    }
        ?: return null

    return Uri(
        when (uriLookup) {
            is UriLookup.Timeline.FeedGenerator -> "${profileDid.did}/$FeedGeneratorCollection/${uriLookup.feedUriSuffix}"
            is UriLookup.Timeline.List -> "${profileDid.did}/$ListCollection/${uriLookup.listUriSuffix}"
            is UriLookup.Post -> "${profileDid.did}/$PostCollection${uriLookup.postUriSuffix}"
            is UriLookup.Profile -> profileDid.did
        }
    )
}

private const val PostCollection = "app.bsky.feed.post"
private const val ListCollection = "app.bsky.graph.list"
private const val FeedGeneratorCollection = "app.bsky.feed.generator"