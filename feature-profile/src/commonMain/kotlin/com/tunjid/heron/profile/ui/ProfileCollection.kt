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

package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.profile.Action
import com.tunjid.heron.profile.ProfileCollection
import com.tunjid.heron.profile.ProfileCollection.OfFeedGenerators
import com.tunjid.heron.profile.ProfileCollection.OfLists
import com.tunjid.heron.profile.ProfileCollection.OfStarterPacks
import com.tunjid.heron.profile.ProfileCollectionStateHolder
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.list.FeedList
import com.tunjid.heron.timeline.ui.list.StarterPack
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope

@Composable
internal fun ProfileCollection(
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    collectionStateHolder: ProfileCollectionStateHolder,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val collectionState by collectionStateHolder.state.collectAsStateWithLifecycle()
    val updatedItems by rememberUpdatedState(collectionState.tiledItems)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
    ) {
        items(
            items = updatedItems,
            key = ProfileCollection::id,
            itemContent = { profileCollection ->
                val onCollectionClicked = { collection: ProfileCollection ->
                    actions(
                        Action.Navigate.To(
                            pathDestination(
                                path = collection.uriPath,
                                models = listOf(
                                    when (collection) {
                                        is OfFeedGenerators -> collection.feedGenerator
                                        is OfLists -> collection.list
                                        is OfStarterPacks -> collection.starterPack
                                    }
                                ),
                                sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                                referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                            )
                        )
                    )
                }
                when (profileCollection) {
                    is OfFeedGenerators -> FeedGenerator(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .animateItem(),
                        movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                        feedGenerator = profileCollection.feedGenerator,
                        status = com.tunjid.heron.data.core.models.FeedGenerator.Status.None,
                        onFeedGeneratorClicked = {
                            onCollectionClicked(profileCollection)
                        },
                    )

                    is OfLists -> FeedList(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .animateItem(),
                        movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                        list = profileCollection.list,
                        onListClicked = {
                            onCollectionClicked(profileCollection)
                        },
                    )

                    is OfStarterPacks -> StarterPack(
                        modifier = Modifier
                            .fillMaxWidth(),
                        movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
                        starterPack = profileCollection.starterPack,
                        onStarterPackClicked = {
                            onCollectionClicked(profileCollection)
                        },
                    )
                }
            }
        )
    }

    listState.PivotedTilingEffect(
        items = updatedItems,
        onQueryChanged = { query ->
            collectionStateHolder.accept(
                TilingState.Action.LoadAround(
                    query = query ?: collectionState.tilingData.currentQuery
                )
            )
        }
    )
}

private val ProfileCollection.uriPath
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.uri.path
        is OfLists -> list.uri.path
        is OfStarterPacks -> starterPack.uri.path
    }

private const val ProfileCollectionSharedElementPrefix = "profile-collection"