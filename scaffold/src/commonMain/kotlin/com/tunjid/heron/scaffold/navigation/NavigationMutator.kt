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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.UrlEncodableModel
import com.tunjid.heron.data.core.models.fromBase64EncodedUrl
import com.tunjid.heron.data.core.models.toUrlEncodedBase64
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.data.repository.EmptySavedState
import com.tunjid.heron.data.repository.InitialSavedState
import com.tunjid.heron.data.repository.SavedState
import com.tunjid.heron.data.repository.SavedStateRepository
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption.Companion.referringRouteQueryParams
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.StackNav
import com.tunjid.treenav.pop
import com.tunjid.treenav.push
import com.tunjid.treenav.strings.Route
import com.tunjid.treenav.strings.RouteParams
import com.tunjid.treenav.strings.RouteParser
import com.tunjid.treenav.strings.optionalMappedRouteQuery
import com.tunjid.treenav.strings.optionalRouteQuery
import com.tunjid.treenav.strings.routeOf
import com.tunjid.treenav.strings.routeQuery
import com.tunjid.treenav.strings.routeString
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.auth
import heron.scaffold.generated.resources.home
import heron.scaffold.generated.resources.messages
import heron.scaffold.generated.resources.notifications
import heron.scaffold.generated.resources.search
import heron.scaffold.generated.resources.splash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface NavigationStateHolder : ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>>
typealias NavigationMutation = NavigationContext.() -> MultiStackNav

val Route.model: UrlEncodableModel? by optionalMappedRouteQuery(
    mapper = String::fromBase64EncodedUrl
)

val Route.avatarSharedElementKey by optionalRouteQuery()

@OptIn(ExperimentalUuidApi::class)
val Route.sharedElementPrefix by routeQuery(
    default = Uuid.random().toHexString(),
)

fun profileDestination(
    profile: Profile,
    avatarSharedElementKey: String?,
    referringRouteOption: NavigationAction.ReferringRouteOption,
): NavigationAction.Destination = pathDestination(
    path = "/profile/${profile.did.id}",
    model = profile,
    sharedElementPrefix = null,
    avatarSharedElementKey = avatarSharedElementKey,
    referringRouteOption = referringRouteOption,
)

fun postDestination(
    post: Post,
    sharedElementPrefix: String,
    referringRouteOption: NavigationAction.ReferringRouteOption,
): NavigationAction.Destination = pathDestination(
    path = post.uri.path,
    model = post,
    sharedElementPrefix = sharedElementPrefix,
    referringRouteOption = referringRouteOption,
)

fun pathDestination(
    path: String,
    model: UrlEncodableModel? = null,
    sharedElementPrefix: String? = null,
    avatarSharedElementKey: String? = null,
    referringRouteOption: NavigationAction.ReferringRouteOption,
): NavigationAction.Destination = NavigationAction.Destination.ToRawUrl(
    path = path,
    model = model,
    sharedElementPrefix = sharedElementPrefix,
    avatarSharedElementKey = avatarSharedElementKey,
    referringRouteOption = referringRouteOption,
)

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

    sealed interface Destination : NavigationAction {

        data class ComposePost(
            val type: Post.Create,
            val sharedElementPrefix: String?,
        ) : Destination {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/compose",
                        queryParams = mapOf(
                            "creationType" to listOf(type.toUrlEncodedBase64()),
                            "sharedElementPrefix" to listOfNotNull(sharedElementPrefix),
                        )
                    ).toRoute
                )
            }
        }

        data class ToMedia(
            val post: Post,
            val media: Embed.Media,
            val startIndex: Int,
            val sharedElementPrefix: String,
        ) : Destination {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/post/${post.cid.id}/gallery",
                        queryParams = mapOf(
                            "post" to listOf(post.toUrlEncodedBase64()),
                            "media" to listOf(media.toUrlEncodedBase64()),
                            "startIndex" to listOf(startIndex.toString()),
                            "sharedElementPrefix" to listOf(sharedElementPrefix),
                        )
                    ).toRoute
                )
            }
        }

        data class ToRawUrl(
            val path: String,
            val model: UrlEncodableModel? = null,
            val sharedElementPrefix: String? = null,
            val avatarSharedElementKey: String? = null,
            val referringRouteOption: ReferringRouteOption,
        ) : Destination {
            override val navigationMutation: NavigationMutation = {
                routeString(
                    path = path,
                    queryParams = mapOf(
                        "model" to listOfNotNull(model?.toUrlEncodedBase64()),
                        "sharedElementPrefix" to listOfNotNull(sharedElementPrefix),
                        "avatarSharedElementKey" to listOfNotNull(avatarSharedElementKey),
                        referringRouteQueryParams(referringRouteOption),
                    )
                ).toRoute
                    .takeIf { it.id != currentRoute.id }
                    ?.let(navState::push)
                    ?: navState
            }
        }

        data class ToConversation(
            val id: ConversationId,
            val members: List<Profile> = emptyList(),
            val sharedElementPrefix: String? = null,
            val referringRouteOption: ReferringRouteOption,
        ) : Destination {
            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = "/messages/${id.id}",
                        queryParams = mapOf(
                            "model" to members.map { it.toUrlEncodedBase64() },
                            "sharedElementPrefix" to listOfNotNull(sharedElementPrefix),
                            referringRouteQueryParams(referringRouteOption),
                        )
                    ).toRoute
                )
            }
        }

        sealed class ToProfiles : Destination {
            abstract val profileId: ProfileId

            sealed class Post : ToProfiles() {
                data class Likes(
                    override val profileId: ProfileId,
                    val postRecordKey: RecordKey,
                ) : ToProfiles.Post()

                data class Repost(
                    override val profileId: ProfileId,
                    val postRecordKey: RecordKey,
                ) : ToProfiles.Post()
            }

            sealed class Profile : ToProfiles() {
                data class Followers(
                    override val profileId: ProfileId,
                ) : ToProfiles.Profile()

                data class Following(
                    override val profileId: ProfileId,
                ) : ToProfiles.Profile()
            }

            override val navigationMutation: NavigationMutation = {
                navState.push(
                    routeString(
                        path = when (this@ToProfiles) {
                            is ToProfiles.Post.Likes -> "/profile/${profileId.id}/post/${postRecordKey.value}/liked-by"
                            is ToProfiles.Post.Repost -> "/profile/${profileId.id}/post/${postRecordKey.value}/reposted-by"
                            is ToProfiles.Profile.Followers -> "/profile/${profileId.id}/followers"
                            is ToProfiles.Profile.Following -> "/profile/${profileId.id}/follows"
                        },
                        queryParams = mapOf(
                            referringRouteQueryParams(ReferringRouteOption.Current),
                        )
                    ).toRoute
                )
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
            private const val QueryParam = "referringRoute"

            fun NavigationContext.referringRouteQueryParams(
                option: ReferringRouteOption,
            ): Pair<String, List<String>> = QueryParam to when (option) {
                Current -> listOf(
                    currentRoute.encodeToQueryParam()
                )

                Parent -> currentRoute
                    .routeParams
                    .queryParams
                    .getOrElse(
                        key = QueryParam,
                        defaultValue = ::emptyList,
                    )

                ParentOrCurrent -> referringRouteQueryParams(Parent).second.ifEmpty {
                    referringRouteQueryParams(Current).second
                }
            }

            fun RouteParams.decodeReferringRoute() =
                queryParams[QueryParam]?.firstOrNull()
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
    @Named("AppScope") appScope: CoroutineScope,
    savedStateRepository: SavedStateRepository,
    routeParser: RouteParser,
) : NavigationStateHolder,
    ActionStateMutator<NavigationMutation, StateFlow<MultiStackNav>> by appScope.actionStateFlowMutator(
        initialState = InitialNavigationState,
        started = SharingStarted.Eagerly,
        inputs = listOf(
            savedStateRepository.forceSignOutMutations()
        ),
        actionTransform = { navActions ->
            flow {
                // Restore saved nav from disk first
                val savedState = savedStateRepository.savedState
                    // Wait for a non empty saved state to be read
                    .first { it != InitialSavedState }

                val multiStackNav = when {
                    savedState == EmptySavedState -> SignedOutNavigationState
                    savedState.auth == null -> SignedOutNavigationState
                    else -> routeParser.parseMultiStackNav(savedState)
                }

                emit { multiStackNav }

                emitAll(
                    navActions.mapToMutation { navMutation ->
                        navMutation(
                            ImmutableNavigationContext(
                                state = this,
                                routeParser = routeParser
                            )
                        )
                    }
                )
            }
        },
        stateTransform = { navigationStateFlow ->
            // Save each new navigation state in parallel
            navigationStateFlow.onEach { navigationState ->
                appScope.persistNavigationState(
                    navigationState = navigationState,
                    savedStateRepository = savedStateRepository
                )
            }
        }
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

private fun SavedStateRepository.forceSignOutMutations(): Flow<Mutation<MultiStackNav>> =
    savedState
        // No auth token and is displaying main navigation
        .filter { it.auth == null && it != EmptySavedState }
        .mapToMutation { _ ->
            SignedOutNavigationState
        }

private fun CoroutineScope.persistNavigationState(
    navigationState: MultiStackNav,
    savedStateRepository: SavedStateRepository,
) = launch {
    if (navigationState != InitialNavigationState) savedStateRepository.updateState {
        this.copy(navigation = navigationState.toSavedState())
    }
}

private val SavedState.isSignedIn get() = auth != null

private fun RouteParser.parseMultiStackNav(savedState: SavedState) =
    savedState.navigation.backStacks
        .foldIndexed(
            initial = MultiStackNav(
                name = if (savedState.isSignedIn) SignedInNavigationState.name
                else SignedOutNavigationState.name
            ),
            operation = { index, multiStackNav, routesForStack ->
                multiStackNav.copy(
                    stacks = multiStackNav.stacks +
                            routesForStack.fold(
                                initial = StackNav(
                                    name = when {
                                        savedState.isSignedIn -> SignedInNavigationState
                                        else -> SignedOutNavigationState
                                    }.stacks.getOrNull(index)?.name ?: "Unknown"
                                ),
                                operation = innerFold@{ stackNav, route ->
                                    val resolvedRoute =
                                        parse(pathAndQueries = route) ?: unknownRoute()
                                    stackNav.copy(
                                        children = stackNav.children + resolvedRoute
                                    )
                                }
                            )
                )
            }
        )
        .copy(
            currentIndex = savedState.navigation.activeNav
        )

private fun MultiStackNav.toSavedState() = SavedState.Navigation(
    activeNav = currentIndex,
    backStacks = stacks.fold(listOf()) { listOfLists, stackNav ->
        listOfLists.plus(
            element = stackNav.children
                .filterIsInstance<Route>()
                .fold(listOf()) { stackList, route ->
                    stackList + route.routeParams.pathAndQueries
                }
        )
    },
)

private val InitialNavigationState = MultiStackNav(
    name = "splash-app",
    stacks = listOf(
        AppStack.Splash.toStackNav()
    )
)

private val SignedOutNavigationState = MultiStackNav(
    name = "signed-out-app",
    stacks = listOf(
        AppStack.Auth.toStackNav()
    )
)

private val SignedInNavigationState = MultiStackNav(
    name = "signed-in-app",
    stacks = listOf(
        AppStack.Home,
        AppStack.Search,
        AppStack.Messages,
        AppStack.Notifications,
    ).map(AppStack::toStackNav)
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
    );
}

private fun AppStack.toStackNav() = StackNav(
    name = stackName,
    children = listOf(rootRoute)
)