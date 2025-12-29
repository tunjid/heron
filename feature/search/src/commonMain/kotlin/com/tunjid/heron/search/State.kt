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

import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.Trend
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.data.repository.SearchQuery
import com.tunjid.heron.data.repository.SearchQuery.OfPosts
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.search.ui.suggestions.SuggestedStarterPack
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.ActionStateMutator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class ScreenLayout {
    Suggested,
    AutoCompleteProfiles,
    GeneralSearchResults,
}

internal typealias SearchResultStateHolder = ActionStateMutator<SearchState.Tile, out StateFlow<SearchState>>

sealed interface SearchResult {

    data class OfProfile(
        val profileWithViewerState: ProfileWithViewerState,
    ) : SearchResult

    data class OfFeedGenerator(
        val feedGenerator: FeedGenerator,
    ) : SearchResult

    data class OfPost(
        val timelineItem: TimelineItem,
    ) : SearchResult
}

val SearchResult.OfPost.id: String
    get() = timelineItem.id

val SearchResult.OfPost.canAutoPlayVideo: Boolean
    get() = timelineItem.appliedLabels.canAutoPlayVideo

sealed class SearchState {
    data class OfPosts(
        override val tilingData: TilingState.Data<SearchQuery.OfPosts, SearchResult.OfPost>,
    ) : SearchState(),
        TilingState<SearchQuery.OfPosts, SearchResult.OfPost>

    data class OfProfiles(
        override val tilingData: TilingState.Data<SearchQuery.OfProfiles, SearchResult.OfProfile>,
    ) : SearchState(),
        TilingState<SearchQuery.OfProfiles, SearchResult.OfProfile>

    data class OfFeedGenerators(
        override val tilingData: TilingState.Data<SearchQuery.OfFeedGenerators, SearchResult.OfFeedGenerator>,
    ) : SearchState(),
        TilingState<SearchQuery.OfFeedGenerators, SearchResult.OfFeedGenerator>

    data class Tile(
        val tilingAction: TilingState.Action,
    )
}

val SearchState.key
    get() = when (this) {
        is SearchState.OfFeedGenerators -> "feed-generators"
        is SearchState.OfPosts -> when (tilingData.currentQuery) {
            is OfPosts.Latest -> "latest-posts"
            is OfPosts.Top -> "top-posts"
        }
        is SearchState.OfProfiles -> "profiles"
    }

val SearchState.sharedElementPrefix
    get() = key

@Serializable
data class State(
    val currentQuery: String = "",
    val layout: ScreenLayout = ScreenLayout.Suggested,
    val signedInProfile: Profile? = null,
    val trends: List<Trend> = emptyList(),
    val suggestedProfileCategory: String? = null,
    val isQueryEditable: Boolean = true,
    val timelineRecordUrisToPinnedStatus: Map<RecordUri?, Boolean> = emptyMap(),
    @Transient
    val preferences: Preferences = Preferences.EmptyPreferences,
    @Transient
    val categoriesToSuggestedProfiles: Map<String?, List<ProfileWithViewerState>> = emptyMap(),
    @Transient
    val recentConversations: List<Conversation> = emptyList(),
    @Transient
    val starterPacksWithMembers: List<SuggestedStarterPack> = emptyList(),
    @Transient
    val feedGenerators: List<FeedGenerator> = emptyList(),
    @Transient
    val searchStateHolders: List<SearchResultStateHolder> = emptyList(),
    @Transient
    val autoCompletedProfiles: List<SearchResult.OfProfile> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

sealed class Action(val key: String) {

    sealed class Search : Action(key = "Search") {
        data class OnSearchQueryChanged(
            val query: String,
        ) : Search()

        data class OnSearchQueryConfirmed(
            val isLocalOnly: Boolean,
        ) : Search()
    }

    data class FetchSuggestedProfiles(
        val category: String? = null,
    ) : Action(key = "FetchSuggestedProfiles")

    data class UpdateMutedWord(
        val mutedWordPreferences: List<MutedWordPreference>,
    ) : Action(key = "UpdateMutedWord")

    data class SendPostInteraction(
        val interaction: Post.Interaction,
    ) : Action(key = "SendPostInteraction")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data class ToggleViewerState(
        val signedInProfileId: ProfileId,
        val viewedProfileId: ProfileId,
        val following: GenericUri?,
        val followedBy: GenericUri?,
    ) : Action(key = "ToggleViewerState")

    data class UpdateFeedGeneratorStatus(
        val update: Timeline.Update,
    ) : Action(key = "UpdateFeedGeneratorStatus")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {

        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
