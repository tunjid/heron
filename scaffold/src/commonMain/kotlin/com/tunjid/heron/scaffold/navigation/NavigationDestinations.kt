package com.tunjid.heron.scaffold.navigation

import com.tunjid.heron.data.core.models.AtmosphereApp
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.UrlEncodableModel
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.utilities.path
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.requireCurrent
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.routeString

fun profileDestination(
    profile: Profile,
    avatarSharedElementKey: String?,
    referringRouteOption: NavigationAction.ReferringRouteOption,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profile.did.id}",
    models = listOf(profile),
    sharedElementPrefix = null,
    avatarSharedElementKey = avatarSharedElementKey,
    referringRouteOption = referringRouteOption,
)

fun recordDestination(
    record: Record,
    otherModels: List<UrlEncodableModel> = emptyList(),
    sharedElementPrefix: String,
    referringRouteOption: NavigationAction.ReferringRouteOption,
): NavigationAction.Destination = pathDestination(
    path = record.reference.uri.path,
    models = buildList {
        if (record is UrlEncodableModel) add(record)
        addAll(otherModels)
    },
    sharedElementPrefix = sharedElementPrefix,
    referringRouteOption = referringRouteOption,
)

fun composePostDestination(
    type: Post.Create? = null,
    sharedElementPrefix: String? = null,
    sharedUri: GenericUri? = null,
): NavigationAction.Destination = pathDestination(
    path = "/compose",
    models = listOfNotNull(type),
    sharedUri = sharedUri,
    sharedElementPrefix = sharedElementPrefix,
)

fun conversationDestination(
    id: ConversationId,
    members: List<Profile> = emptyList(),
    sharedElementPrefix: String? = null,
    sharedUri: GenericUri? = null,
    referringRouteOption: NavigationAction.ReferringRouteOption,
): NavigationAction.Destination = pathDestination(
    path = "/messages/${id.id}",
    models = members,
    sharedUri = sharedUri,
    sharedElementPrefix = sharedElementPrefix,
    referringRouteOption = referringRouteOption,
)

fun editProfileDestination(
    profile: Profile,
    avatarSharedElementKey: String?,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profile.did.id}/edit",
    models = listOf(profile),
    avatarSharedElementKey = avatarSharedElementKey,
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun galleryDestination(
    post: Post,
    media: Embed.Media,
    startIndex: Int,
    sharedElementPrefix: String,
    otherModels: List<UrlEncodableModel> = emptyList(),
): NavigationAction.Destination = pathDestination(
    path = "/profile/${post.author.did.id}/post/${post.uri.recordKey.value}/gallery",
    models = buildList {
        add(media)
        addAll(otherModels)
    },
    sharedElementPrefix = sharedElementPrefix,
    miscQueryParams = mapOf(
        "startIndex" to listOf(startIndex.toString()),
    ),
)

fun postLikesDestination(
    profileId: ProfileId,
    postRecordKey: RecordKey,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/post/${postRecordKey.value}/liked-by",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun postQuotesDestination(
    profileId: ProfileId,
    postRecordKey: RecordKey,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/post/${postRecordKey.value}/quotes",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun postRepostsDestination(
    profileId: ProfileId,
    postRecordKey: RecordKey,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/post/${postRecordKey.value}/reposted-by",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun bookmarksDestination(): NavigationAction.Destination = pathDestination(
    path = "/saved",
)

fun profileFollowsDestination(
    profileId: ProfileId,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/follows",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun profileFollowersDestination(
    profileId: ProfileId,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/followers",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun searchProfilePostsDestination(
    profile: Profile,
): NavigationAction.Destination = pathDestination(
    path = "/search/from:${profile.handle.id}",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun signInDestination(): NavigationAction.Destination = pathDestination(
    path = "/auth",
)

fun settingsDestination(): NavigationAction.Destination = pathDestination(
    path = "/settings",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun notificationSettingsDestination(): NavigationAction.Destination = pathDestination(
    path = "/settings/notifications",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun tasksDestination(
    showFailedWrites: Boolean = false,
): NavigationAction.Destination = pathDestination(
    path = "/tasks",
    miscQueryParams = mapOf(
        "showFailedWrites" to listOf(showFailedWrites.toString()),
    ),
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun moderationDestination(): NavigationAction.Destination = pathDestination(
    path = "/moderation",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun blocksDestination(): NavigationAction.Destination = pathDestination(
    path = "/moderation/blocked-accounts",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun mutesDestination(): NavigationAction.Destination = pathDestination(
    path = "/moderation/muted-accounts",
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun grazeEditorDestination(
    feedGenerator: FeedGenerator? = null,
    sharedElementPrefix: String? = null,
): NavigationAction.Destination = pathDestination(
    path = when (feedGenerator) {
        null -> "/graze/create"
        else -> "/graze/edit/${feedGenerator.uri.recordKey.value}"
    },
    models = listOfNotNull(
        feedGenerator,
    ),
    sharedElementPrefix = sharedElementPrefix,
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun standardPublicationDestination(
    publication: StandardPublication,
    sharedElementPrefix: String?,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${publication.publisher.did.id}/standard/publication/${publication.uri.recordKey.value}",
    models = listOfNotNull(
        publication,
    ),
    sharedElementPrefix = sharedElementPrefix,
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun atmosphereAppDestination(
    profile: Profile,
    app: AtmosphereApp,
    avatarSharedElementKey: String? = null,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profile.did.id}/app/${app.id}",
    models = listOfNotNull(
        profile,
        app,
    ),
    avatarSharedElementKey = avatarSharedElementKey,
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun standardSubscriptionsDestination(
    sharedElementPrefix: String? = null,
): NavigationAction.Destination = pathDestination(
    path = "/standard/subscriptions",
    sharedElementPrefix = sharedElementPrefix,
    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
)

fun pathDestination(
    path: String,
    models: List<UrlEncodableModel> = emptyList(),
    sharedElementPrefix: String? = null,
    avatarSharedElementKey: String? = null,
    sharedUri: GenericUri? = null,
    miscQueryParams: Map<String, List<String>> = emptyMap(),
    referringRouteOption: NavigationAction.ReferringRouteOption = NavigationAction.ReferringRouteOption.Current,
): NavigationAction.Destination = NavigationAction.Destination.ToRawUrl(
    path = path.stripBeforePath(),
    models = models,
    sharedElementPrefix = sharedElementPrefix,
    avatarSharedElementKey = avatarSharedElementKey,
    sharedUri = sharedUri,
    miscQueries = miscQueryParams,
    referringRouteOption = referringRouteOption,
)

fun removeQueryParamsFromCurrentRoute(
    params: Set<String>,
): NavigationMutation = {
    val current = navState.requireCurrent<Route>()
    if (current.routeParams.queryParams.none { (key) -> params.contains(key) }) navState
    else navState
        .pop()
        .push(
            routeString(
                path = current.routeParams.pathAndQueries.substringBefore('?'),
                queryParams = current.routeParams.queryParams.filterKeys { it !in params },
            ).toRoute,
        )
}

internal fun deepLinkTo(
    deepLink: GenericUri,
): NavigationMutation = {
    when {
        deepLink.uri.lowercase().contains(NavigationState.OAuthUrlPathSegment) -> navState.copy(
            stacks = navState.stacks.mapIndexed { index, stackNav ->
                if (index == navState.currentIndex) stackNav.copy(
                    children = listOf(deepLink.uri.toRoute),
                )
                else stackNav
            },
        )
        else -> navState.push(deepLink.uri.toRoute)
    }
}

private fun String.stripBeforePath(): String {
    // 1. Get the part after "://" or the whole string if "://" is not present.
    // "https://example.com/path" -> "example.com/path"
    // "example.com/path"         -> "example.com/path"
    // "/path"                    -> "/path"
    val authorityAndPath = this.substringAfter(
        delimiter = "://",
        missingDelimiterValue = this,
    )

    // 2. Find the first slash in that remaining part.
    // "example.com/path" -> index 11
    // "/path"            -> index 0
    // "example.com"      -> index -1
    val pathStartIndex = authorityAndPath.indexOf('/')

    // 3. Return the substring from that slash.
    // If no slash is found (index -1), return the home route.
    return if (pathStartIndex != -1) authorityAndPath.substring(pathStartIndex)
    else AppStack.Home.rootRoute.id
}
