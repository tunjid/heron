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

package com.tunjid.heron.scaffold.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Start
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.UrlEncodableModel
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.data.di.AppMainScope
import com.tunjid.heron.data.repository.AuthRepository
import com.tunjid.heron.data.repository.EmptyNavigation
import com.tunjid.heron.data.repository.InitialNavigation
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToMutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.requireCurrent
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routeQuery
import com.tunjid.treenav.strings.routeString
import dev.zacsweers.metro.Inject
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.auth
import heron.scaffold.generated.resources.home
import heron.scaffold.generated.resources.messages
import heron.scaffold.generated.resources.notifications
import heron.scaffold.generated.resources.search
import heron.scaffold.generated.resources.splash
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

interface NavigationStateHolder : ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

val Route.sharedUri: GenericUri? by optionalMappedRouteQuery(
    mapper = ::GenericUri,
)

inline fun <reified T> Route.model(): T? = models.asSequence()
    .filterIsInstance<T>()
    .firstOrNull()

val Route.models: List<UrlEncodableModel>
    get() = routeParams.queryParams["model"]
        ?.map(String::fromBase64EncodedUrl)
        ?: emptyList()

val Route.avatarSharedElementKey by optionalRouteQuery()

@OptIn(ExperimentalUuidApi::class)
val Route.sharedElementPrefix by routeQuery(
    default = Uuid.random().toHexString(),
)

fun profileDestination(
    profile: Profile,
    avatarSharedElementKey: String?,
    referringRouteOption: ReferringRouteOption,
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
    referringRouteOption: ReferringRouteOption,
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
    referringRouteOption: ReferringRouteOption,
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
    referringRouteOption = ReferringRouteOption.Current,
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
    referringRouteOption = ReferringRouteOption.Current,
)

fun postQuotesDestination(
    profileId: ProfileId,
    postRecordKey: RecordKey,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/post/${postRecordKey.value}/quotes",
    referringRouteOption = ReferringRouteOption.Current,
)

fun postRepostsDestination(
    profileId: ProfileId,
    postRecordKey: RecordKey,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/post/${postRecordKey.value}/reposted-by",
    referringRouteOption = ReferringRouteOption.Current,
)

fun bookmarksDestination(): NavigationAction.Destination = pathDestination(
    path = "/saved",
)

fun profileFollowsDestination(
    profileId: ProfileId,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/follows",
    referringRouteOption = ReferringRouteOption.Current,
)

fun profileFollowersDestination(
    profileId: ProfileId,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profileId.id}/followers",
    referringRouteOption = ReferringRouteOption.Current,
)

fun signInDestination(): NavigationAction.Destination = pathDestination(
    path = "/auth",
)

fun settingsDestination(): NavigationAction.Destination = pathDestination(
    path = "/settings",
    referringRouteOption = ReferringRouteOption.Current,
)

fun notificationSettingsDestination(): NavigationAction.Destination = pathDestination(
    path = "/settings/notifications",
    referringRouteOption = ReferringRouteOption.Current,
)

fun moderationDestination(): NavigationAction.Destination = pathDestination(
    path = "/moderation",
    referringRouteOption = ReferringRouteOption.Current,
)

fun blocksDestination(): NavigationAction.Destination = pathDestination(
    path = "/moderation/blocked-accounts",
    referringRouteOption = ReferringRouteOption.Current,
)

fun mutesDestination(): NavigationAction.Destination = pathDestination(
    path = "/moderation/muted-accounts",
    referringRouteOption = ReferringRouteOption.Current,
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
    referringRouteOption = ReferringRouteOption.Current,
)

fun pathDestination(
    path: String,
    models: List<UrlEncodableModel> = emptyList(),
    sharedElementPrefix: String? = null,
    avatarSharedElementKey: String? = null,
    sharedUri: GenericUri? = null,
    miscQueryParams: Map<String, List<String>> = emptyMap(),
    referringRouteOption: ReferringRouteOption = ReferringRouteOption.Current,
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
        deepLink.uri.lowercase().contains(OAuthUrlPathSegment) -> navState.copy(
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

/**
 * An action that causes mutations to navigation
 */
interface NavigationAction {
    val navigationMutation: NavigationMutation

    data object Pop : NavigationAction {
        override val navigationMutation: NavigationMutation = {
            navState.pop()
        }
    }

    sealed class Destination : NavigationAction {

        internal data class ToRawUrl(
            val path: String,
            val sharedElementPrefix: String? = null,
            val avatarSharedElementKey: String? = null,
            val sharedUri: GenericUri? = null,
            val models: List<UrlEncodableModel> = emptyList(),
            val miscQueries: Map<String, List<String>> = emptyMap(),
            val referringRouteOption: ReferringRouteOption,
        ) : Destination() {
            override val navigationMutation: NavigationMutation = {
                routeString(
                    path = path,
                    queryParams = miscQueries + mapOf(
                        "model" to models.map(UrlEncodableModel::toUrlEncodedBase64),
                        "sharedElementPrefix" to listOfNotNull(sharedElementPrefix),
                        "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                        "sharedUri" to listOfNotNull(sharedUri?.uri),
                        referringRouteQueryParams(referringRouteOption),
                    ),
                ).toRoute
                    .takeIf { it.id != currentRoute.id }
                    ?.let(navState::push)
                    ?: navState
            }
        }
    }

    /**
     * Definition for describing what route referred a route to a destination
     */
    sealed class ReferringRouteOption {

        /**
         * The current route is the referrer.
         */
        data object Current : ReferringRouteOption()

        /**
         * The referrer is the route that referred the current route if it exists.
         */
        data object Parent : ReferringRouteOption()

        /**
         * The parent referrer is the referrer it it exists, otherwise
         * the current route is the referrer.
         */
        data object ParentOrCurrent : ReferringRouteOption()

        companion object {
            fun NavigationContext.referringRouteQueryParams(
                option: ReferringRouteOption,
            ): Pair<String, List<String>> = ReferringRouteQueryParam to when (option) {
                Current -> listOf(
                    currentRoute.encodeToQueryParam(),
                )

                Parent ->
                    currentRoute
                        .routeParams
                        .queryParams
                        .getOrElse(
                            key = ReferringRouteQueryParam,
                            defaultValue = ::emptyList,
                        )

                ParentOrCurrent -> referringRouteQueryParams(Parent).second.ifEmpty {
                    referringRouteQueryParams(Current).second
                }
            }

            fun RouteParams.decodeReferringRoute() =
                queryParams[ReferringRouteQueryParam]?.firstOrNull()
                    ?.decodeRoutePathAndQueriesFromQueryParam()
                    ?.let(::routeOf)

            /**
             * Hydrates a route with metadata that may have been lost like path args and
             * query args.
             */
            fun RouteParser.hydrate(route: Route) = parse(route.routeParams.pathAndQueries) ?: route
        }
    }
}

@Inject
class PersistedNavigationStateHolder(
    @AppMainScope
    appMainScope: CoroutineScope,
    userDataRepository: UserDataRepository,
    authRepository: AuthRepository,
    routeParser: RouteParser,
) : NavigationStateHolder,
    ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>> by appMainScope.actionStateFlowMutator(
        initialState = InitialNavigationState,
        started = SharingStarted.Eagerly,
        actionTransform = { navActions ->
            flow {
                val startTime = TimeSource.Monotonic.markNow()

                // Restore saved nav from disk first
                val savedNavigation = userDataRepository.navigation
                    // Wait for a non empty saved state to be read
                    .first { it != InitialNavigation }

                val isSignedIn = authRepository.isSignedIn.first()

                val multiStackNav = when {
                    savedNavigation == EmptyNavigation -> SignedOutNavigationState
                    !isSignedIn -> SignedOutNavigationState
                    else -> routeParser.parseMultiStackNav(
                        navigation = savedNavigation,
                        isSignedIn = isSignedIn,
                    ).let {
                        val wasInOauthFlow = it.current?.id?.contains(OAuthUrlPathSegment) == true
                        if (wasInOauthFlow) SignedOutNavigationState else it
                    }
                }

                val elapsed = startTime.elapsedNow()
                if (elapsed < SplashDelay) delay(SplashDelay - elapsed)

                emit { multiStackNav }

                emitAll(
                    merge(
                        navActions.mapToMutation { navMutation ->
                            navMutation(
                                ImmutableNavigationContext(
                                    state = this,
                                    routeParser = routeParser,
                                ),
                            )
                        },
                        forceSignOutMutations(
                            authRepository = authRepository,
                            userDataRepository = userDataRepository,
                        ),
                    ),
                )
            }
        },
        stateTransform = { navigationStateFlow ->
            // Save each new navigation state in parallel
            navigationStateFlow.onEach { navigationState ->
                // Fire and forget, do not slow down the collector,
                // navigation needs to be immediate.
                appMainScope.persistNavigationState(
                    navigationState = navigationState,
                    userDataRepository = userDataRepository,
                )
            }
        },
    )

@Suppress("UnusedReceiverParameter")
fun NavigationContext.resetAuthNavigation(): MultiStackNav =
    SignedInNavigationState

/**
 * A helper function for generic state producers to consume navigation actions
 */
fun <Action : NavigationAction, State> Flow<Action>.consumeNavigationActions(
    navigationMutationConsumer: (NavigationMutation) -> Unit,
) = flatMapLatest { action ->
    navigationMutationConsumer(action.navigationMutation)
    emptyFlow<Mutation<State>>()
}

private fun forceSignOutMutations(
    authRepository: AuthRepository,
    userDataRepository: UserDataRepository,
): Flow<Mutation<MultiStackNav>> =
    combine(
        authRepository.isSignedIn,
        authRepository.isGuest,
        userDataRepository.navigation,
        ::Triple,
    )
        .filter { (isSignedIn, isGuest, navigation) ->
            // No auth token and is displaying main navigation
            !isSignedIn && !isGuest && navigation != EmptyNavigation
        }
        .mapLatestToMutation {
            when (stacks[currentIndex].name) {
                // If on the auth stack already, keep the navigation state as is
                AppStack.Auth.stackName -> this
                else -> SignedOutNavigationState
            }
        }

private fun CoroutineScope.persistNavigationState(
    navigationState: MultiStackNav,
    userDataRepository: UserDataRepository,
) = launch {
    if (navigationState != InitialNavigationState) userDataRepository.persistNavigationState(
        navigation = navigationState.toSavedState(),
    )
}

private fun RouteParser.parseMultiStackNav(
    navigation: SavedState.Navigation,
    isSignedIn: Boolean,
): MultiStackNav {
    val restored = navigation.backStacks
        .foldIndexed(
            initial = MultiStackNav(
                name = if (isSignedIn) SignedInNavigationState.name
                else SignedOutNavigationState.name,
            ),
            operation = { index, multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                        routesForStack.fold(
                            initial = StackNav(
                                name = when {
                                    isSignedIn -> SignedInNavigationState
                                    else -> SignedOutNavigationState
                                }.stacks.getOrNull(index)?.name ?: "Unknown",
                            ),
                            operation = innerFold@{ stackNav, route ->
                                val resolvedRoute =
                                    parse(pathAndQueries = route) ?: unknownRoute()
                                stackNav.copy(
                                    children = stackNav.children + resolvedRoute,
                                )
                            },
                        ),
                )
            },
        )
        .copy(
            currentIndex = navigation.activeNav,
        )

    // Don't put a signed in user on the sign in screen
    return if (restored.current?.id == AppStack.Auth.rootRoute.id) SignedInNavigationState
    else restored
}

private fun MultiStackNav.toSavedState() = SavedState.Navigation(
    activeNav = currentIndex,
    backStacks = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                },
        )
    },
)

internal val MultiStackNav.isShowingSplashScreen
    get() = this == InitialNavigationState

private val InitialNavigationState = MultiStackNav(
    name = "splash-app",
    stacks = listOf(
        AppStack.Splash.toStackNav(),
    ),
)

private val SignedOutNavigationState = MultiStackNav(
    name = "signed-out-app",
    stacks = listOf(
        AppStack.Auth.toStackNav(),
    ),
)

private val SignedInNavigationState = MultiStackNav(
    name = "signed-in-app",
    stacks = listOf(
        AppStack.Home,
        AppStack.Search,
        AppStack.Messages,
        AppStack.Notifications,
    ).map(AppStack::toStackNav),
)

enum class AppStack(
    val stackName: String,
    val titleRes: StringResource,
    val icon: ImageVector,
    val rootRoute: Route,
) {
    Home(
        stackName = "home-stack",
        titleRes = Res.string.home,
        icon = Icons.Rounded.Home,
        rootRoute = routeOf("/home"),
    ),
    Search(
        stackName = "search-stack",
        titleRes = Res.string.search,
        icon = Icons.Rounded.Search,
        rootRoute = routeOf("/search"),
    ),
    Messages(
        stackName = "messages-stack",
        titleRes = Res.string.messages,
        icon = Icons.Rounded.Mail,
        rootRoute = routeOf("/messages"),
    ),
    Notifications(
        stackName = "notifications-stack",
        titleRes = Res.string.notifications,
        icon = Icons.Rounded.Notifications,
        rootRoute = routeOf("/notifications"),
    ),
    Auth(
        stackName = "auth-stack",
        titleRes = Res.string.auth,
        icon = Icons.Rounded.Lock,
        rootRoute = routeOf("/auth"),
    ),
    Splash(
        stackName = "splash-stack",
        titleRes = Res.string.splash,
        icon = Icons.Rounded.Start,
        rootRoute = routeOf("/splash"),
    ),
}

private fun AppStack.toStackNav() = StackNav(
    name = stackName,
    children = listOf(rootRoute),
)

private val SplashDelay = 1.seconds

private const val ReferringRouteQueryParam = "referringRoute"
private const val OAuthUrlPathSegment = "oauth"
