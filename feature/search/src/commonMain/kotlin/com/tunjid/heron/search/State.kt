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

package com.tunjid.heron.search

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.SearchFilter
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.types.FollowUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.ProfileSearchQuery
import com.tunjid.heron.data.repository.records.FeedGeneratorSearchQuery
import com.tunjid.heron.search.ui.suggestions.SuggestedStarterPack
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.timeline.state.TimelineStateHolder
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.getOrNull
import kotlin.text.isNotBlank
import kotlin.text.removePrefix
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ScreenLayout {
    Suggested,
    AutoCompleteProfiles,
    GeneralSearchResults,
}

@Serializable
sealed class RouteQuery {
    data object FullSearch : RouteQuery()
    data class HashtaggedPostsSearch(
        val query: String,
    ) : RouteQuery()

    data class ProfilePostSearch(
        val query: String,
    ) : RouteQuery()
}

val RouteQuery.initialQueryString
    get() = when (this) {
        RouteQuery.FullSearch -> ""
        is RouteQuery.HashtaggedPostsSearch -> query
        is RouteQuery.ProfilePostSearch -> query
    }

val RouteQuery.initialSearchBarText
    get() = when (this) {
        RouteQuery.FullSearch -> ""
        is RouteQuery.HashtaggedPostsSearch -> query
        is RouteQuery.ProfilePostSearch -> ""
    }
val RouteQuery.supportsNonPostSearch
    get() = when (this) {
        RouteQuery.FullSearch -> true
        is RouteQuery.HashtaggedPostsSearch,
        is RouteQuery.ProfilePostSearch,
        -> false
    }

val RouteQuery.initialLayout
    get() = when (this) {
        RouteQuery.FullSearch -> ScreenLayout.Suggested
        is RouteQuery.HashtaggedPostsSearch,
        is RouteQuery.ProfilePostSearch,
        -> ScreenLayout.GeneralSearchResults
    }

val RouteQuery.isQueryEditable
    get() = when (this) {
        RouteQuery.FullSearch -> true
        is RouteQuery.HashtaggedPostsSearch -> false
        is RouteQuery.ProfilePostSearch -> true
    }

val RouteQuery.isRoot
    get() = when (this) {
        RouteQuery.FullSearch -> true
        is RouteQuery.HashtaggedPostsSearch,
        is RouteQuery.ProfilePostSearch,
        -> false
    }

val RouteQuery.ProfilePostSearch.profileHandle
    get() = "@${query.removePrefix("from:")}"

fun RouteQuery.queryString(searchBarText: String) = when (this) {
    RouteQuery.FullSearch -> searchBarText
    is RouteQuery.HashtaggedPostsSearch -> searchBarText
    is RouteQuery.ProfilePostSearch -> "$query $searchBarText"
}

fun RouteQuery.layoutFor(
    action: Action.Search.OnSearchQueryChanged,
): ScreenLayout = when {
    supportsNonPostSearch ->
        if (action.query.isNotBlank()) ScreenLayout.AutoCompleteProfiles
        else ScreenLayout.Suggested
    else -> ScreenLayout.GeneralSearchResults
}

val SearchFilter?.isMediaSearch: Boolean
    get() = when (this?.media) {
        SearchFilter.Media.WithMedia,
        SearchFilter.Media.VideosOnly,
        -> true
        SearchFilter.Media.All,
        null,
        -> false
    }

fun State.presentationOptions(
    currentPage: Int,
): List<Timeline.Presentation> {
    val isMediaSearch =
        searchStateHolders.getOrNull(currentPage) is SearchScreenStateHolders.Posts &&
            appliedFilter.isMediaSearch
    return if (isMediaSearch) Timeline.Presentation.All
    else Timeline.Presentation.TextOnly
}

internal typealias SearchResultStateHolder = ActionSuspendingStateMutator<SearchState.Tile, SearchState>

/**
 * Heterogeneous per-tab state holders for the search screen. Post search is a [Timeline.Search],
 * so the post tabs are full [TimelineStateHolder]s; the profile and feed tabs remain bespoke
 * tiling holders over [SearchState]. Mirrors `ProfileScreenStateHolders`.
 */
@Stable
sealed interface SearchScreenStateHolders {
    val key: String

    @Stable
    class Posts(
        val sort: Timeline.Source.Search.Sort,
        val mutator: TimelineStateHolder,
    ) : SearchScreenStateHolders,
        TimelineStateHolder by mutator {
        override val key: String
            get() = when (sort) {
                Timeline.Source.Search.Sort.Top -> "top-posts"
                Timeline.Source.Search.Sort.Latest -> "latest-posts"
            }
    }

    @Stable
    class Profiles(
        val mutator: SearchResultStateHolder,
    ) : SearchScreenStateHolders,
        SearchResultStateHolder by mutator {
        override val key: String
            get() = mutator.state.key
    }

    @Stable
    class Feeds(
        val mutator: SearchResultStateHolder,
    ) : SearchScreenStateHolders,
        SearchResultStateHolder by mutator {
        override val key: String
            get() = mutator.state.key
    }
}

sealed interface SearchResult {
    data class OfProfile(
        val profileWithViewerState: ProfileWithViewerState,
    ) : SearchResult

    data class OfFeedGenerator(
        val feedGenerator: FeedGenerator,
    ) : SearchResult
}

@Stable
sealed class SearchState {
    data class OfProfiles(
        override val tilingData: TilingState.Data<ProfileSearchQuery, SearchResult.OfProfile>,
    ) : SearchState(),
        TilingState<ProfileSearchQuery, SearchResult.OfProfile>

    data class OfFeedGenerators(
        override val tilingData: TilingState.Data<FeedGeneratorSearchQuery, SearchResult.OfFeedGenerator>,
    ) : SearchState(),
        TilingState<FeedGeneratorSearchQuery, SearchResult.OfFeedGenerator>

    data class Tile(
        val tilingAction: TilingState.Action,
    )
}

val SearchState.key
    get() = when (this) {
        is SearchState.OfFeedGenerators -> "feed-generators"
        is SearchState.OfProfiles -> "profiles"
    }

val SearchState.sharedElementPrefix
    get() = key

@Stable
@Snapshottable
interface State {

    @Serializable
    @SnapshotSpec
    data class Immutable(
        val searchBarText: String = "",
        val query: RouteQuery = RouteQuery.FullSearch,
        val layout: ScreenLayout = ScreenLayout.Suggested,
        val appliedFilter: SearchFilter? = null,
        val draftFilter: SearchFilter = SearchFilter(),
        val preferredPresentation: Timeline.Presentation = Timeline.Presentation.Text.WithEmbed,
        val signedInProfile: Profile? = null,
        val trends: List<Trend> = emptyList(),
        val suggestedProfileCategory: String? = null,
        val timelineRecordUrisToPinnedStatus: Map<RecordUri?, Boolean> = emptyMap(),
        @Transient
        val preferences: Preferences = Preferences.EmptyPreferences,
        @Transient
        val categoriesToSuggestedProfiles: Map<String?, List<ProfileWithViewerState>> = emptyMap(),
        @Transient
        val starterPacksWithMembers: List<SuggestedStarterPack> = emptyList(),
        @Transient
        val feedGenerators: List<FeedGenerator> = emptyList(),
        @Transient
        val searchStateHolders: List<SearchScreenStateHolders> = emptyList(),
        @Transient
        val autoCompletedProfiles: List<SearchResult.OfProfile> = emptyList(),
        @Transient
        val messages: List<Memo> = emptyList(),
    ) : State
}

sealed class Action(val key: String) {

    sealed class Search : Action(key = "Search") {
        data class OnSearchQueryChanged(
            val query: String,
        ) : Search()

        data class OnSearchQueryConfirmed(
            val isLocalOnly: Boolean,
        ) : Search()
    }

    sealed class Filter : Action(key = "Filter") {
        data object Begin : Filter()

        data class Edit(
            val filter: SearchFilter,
        ) : Filter()

        data object Apply : Filter()

        data object Clear : Filter()
    }

    data class FetchSuggestedProfiles(
        val category: String? = null,
    ) : Action(key = "FetchSuggestedProfiles")

    data class BlockAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "BlockAccount")

    data class MuteAccount(
        val signedInProfileId: ProfileId,
        val profileId: ProfileId,
    ) : Action(key = "MuteAccount")

    data class DeleteRecord(
        val recordUri: RecordUri,
    ) : Action(key = "DeleteRecord")

    data class UpdatePresentation(
        val presentation: Timeline.Presentation,
    ) : Action(key = "UpdatePresentation")

    data class TogglePublicationSubscription(
        val publication: StandardPublication,
    ) : Action(key = "TogglePublicationSubscription")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: FollowUri?,
        val followedBy: FollowUri?,
    ) : Action(key = "ToggleViewerState")

    data class UpdateFeedGeneratorStatus(
        val update: Timeline.Update,
    ) : Action(key = "UpdateFeedGeneratorStatus")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {

        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data object Home : Navigate(), NavigationAction by NavigationAction.Home

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
