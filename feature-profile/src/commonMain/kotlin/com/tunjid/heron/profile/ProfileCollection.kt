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

package com.tunjid.heron.profile

import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorList
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.ProfilesQuery
import com.tunjid.heron.data.utilities.CursorQuery
import com.tunjid.heron.data.utilities.cursorListTiler
import com.tunjid.heron.data.utilities.cursorTileInputs
import com.tunjid.heron.data.utilities.ensureValidAnchors
import com.tunjid.heron.data.utilities.isValidFor
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.toTiledList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock


internal typealias ProfileCollectionStateHolder = ActionStateMutator<ProfilesQuery, StateFlow<ProfileCollection>>

sealed class ProfileCollection {
    data class OfFeedGenerators(
        val currentQuery: ProfilesQuery,
        val feedGenerators: TiledList<ProfilesQuery, FeedGenerator> = emptyTiledList(),
    ) : ProfileCollection()

    data class OfStarterPacks(
        val currentQuery: ProfilesQuery,
        val starterPacks: TiledList<ProfilesQuery, StarterPack> = emptyTiledList(),
    ) : ProfileCollection()

    data class OfLists(
        val currentQuery: ProfilesQuery,
        val items: TiledList<ProfilesQuery, FeedList> = emptyTiledList(),
    ) : ProfileCollection()
}

private fun profileCollectionStateHolders(
    coroutineScope: CoroutineScope,
    profileId: Id.Profile,
    profileRepository: ProfileRepository,
): List<ProfileCollectionStateHolder> = listOf(
    ProfileCollection.OfFeedGenerators(
        currentQuery = ProfilesQuery(
            profileId = profileId,
            data = defaultQueryData(),
        ),
        feedGenerators = emptyTiledList(),
    ),
    ProfileCollection.OfStarterPacks(
        currentQuery = ProfilesQuery(
            profileId = profileId,
            data = defaultQueryData(),
        ),
        starterPacks = emptyTiledList(),
    ),
    ProfileCollection.OfFeedGenerators(
        currentQuery = ProfilesQuery(
            profileId = profileId,
            data = defaultQueryData(),
        ),
        feedGenerators = emptyTiledList(),
    ),
).map { searchState ->
    coroutineScope.actionStateFlowMutator(
        initialState = searchState,
        actionTransform = transform@{ actions ->
            actions.toMutationStream {
                when (state()) {
                    is ProfileCollection.OfFeedGenerators -> type().flow.loadMutations(
                        coroutineScope = coroutineScope,
                        cursorListLoader = profileRepository::feedGenerators,
                        profileCollectionMutation = { feedGenerators ->
                            check(this is ProfileCollection.OfFeedGenerators)

                            if (feedGenerators.isValidFor(currentQuery)) copy(
                                feedGenerators = feedGenerators.distinctBy(FeedGenerator::cid)
                            )
                            else this
                        }
                    )

                    is ProfileCollection.OfStarterPacks -> type().flow.loadMutations(
                        coroutineScope = coroutineScope,
                        cursorListLoader = profileRepository::starterPacks,
                        profileCollectionMutation = { starterPacks ->
                            check(this is ProfileCollection.OfStarterPacks)
                            if (starterPacks.isValidFor(currentQuery)) copy(
                                starterPacks = starterPacks.distinctBy(StarterPack::cid)
                            )
                            else this
                        }
                    )

                    is ProfileCollection.OfLists -> type().flow.loadMutations(
                        coroutineScope = coroutineScope,
                        cursorListLoader = profileRepository::lists,
                        profileCollectionMutation = { lists ->
                            check(this is ProfileCollection.OfLists)
                            if (lists.isValidFor(currentQuery)) copy(
                                items = lists.distinctBy(FeedList::cid)
                            )
                            else this
                        }
                    )
                }
            }
        }
    )
}

private inline fun <reified Item> Flow<ProfilesQuery>.loadMutations(
    coroutineScope: CoroutineScope,
    noinline cursorListLoader: (ProfilesQuery, Cursor) -> Flow<CursorList<Item>>,
    noinline profileCollectionMutation: ProfileCollection.(TiledList<ProfilesQuery, Item>) -> ProfileCollection,
): Flow<Mutation<ProfileCollection>> {
    val sharedQueries = ensureValidAnchors()
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
            replay = 1,
        )
    val queryMutations = sharedQueries.mapToMutation<ProfilesQuery, ProfileCollection> { query ->
        when (this) {
            is ProfileCollection.OfFeedGenerators -> copy(currentQuery = query)
            is ProfileCollection.OfLists -> copy(currentQuery = query)
            is ProfileCollection.OfStarterPacks -> copy(currentQuery = query)
        }
    }
    val refreshes = sharedQueries.distinctUntilChangedBy {
        it.data.cursorAnchor
    }
    val itemMutations = refreshes.flatMapLatest { refreshedQuery ->
        cursorTileInputs<ProfilesQuery, Item>(
            numColumns = flowOf(1),
            queries = sharedQueries,
            updatePage = ProfilesQueryUpdater,
        )
            .toTiledList(
                cursorListTiler(
                    startingQuery = refreshedQuery,
                    updatePage = ProfilesQueryUpdater,
                    cursorListLoader = cursorListLoader,
                )
            )
    }
        .mapToMutation(profileCollectionMutation)

    return merge(
        queryMutations,
        itemMutations,
    )
}

private fun defaultQueryData() = CursorQuery.Data(
    page = 0,
    cursorAnchor = Clock.System.now(),
    limit = 15
)

private val ProfilesQueryUpdater: ProfilesQuery.(CursorQuery.Data) -> ProfilesQuery =
    { newData -> copy(data = newData) }