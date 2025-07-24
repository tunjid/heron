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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.utilities.path
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
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
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.BlueskyClouds
import com.tunjid.heron.timeline.utilities.FeedGeneratorCollectionShape
import com.tunjid.heron.timeline.utilities.ListCollectionShape
import com.tunjid.heron.timeline.utilities.StarterPackCollectionShape
import com.tunjid.heron.ui.CollectionLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.collection_by
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
                ProfileCollection(
                    modifier = Modifier
                        .fillMaxWidth(),
                    movableElementSharedTransitionScope = movableElementSharedTransitionScope,
                    title = collectionState.stringResource,
                    collection = profileCollection,
                    onCollectionClicked = { collection ->
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
                    },
                )
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProfileCollection(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    title: StringResource,
    collection: ProfileCollection,
    onCollectionClicked: (ProfileCollection) -> Unit,
) = with(movableElementSharedTransitionScope) {
    CollectionLayout(
        modifier = modifier,
        movableElementSharedTransitionScope = movableElementSharedTransitionScope,
        title = collection.title,
        subtitle = stringResource(
            Res.string.collection_by,
            stringResource(title),
            collection.creator.handle.id,
        ),
        description = collection.description,
        blurb = "",
        sharedElementPrefix = ProfileCollectionSharedElementPrefix,
        sharedElementType = collection.sharedElementType,
        avatar = {
            val avatar = collection.avatar ?: BlueskyClouds
            AsyncImage(
                modifier = Modifier
                    .paneStickySharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = collection.avatarSharedElementKey,
                        )
                    )
                    .size(44.dp),
                args = remember(avatar) {
                    ImageArgs(
                        url = avatar.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = collection.shape,
                    )
                }
            )
        },
        onClicked = {
            onCollectionClicked(collection)
        },
    )
}

private val ProfileCollection.creator
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.creator
        is OfLists -> list.creator
        is OfStarterPacks -> starterPack.creator
    }

private val ProfileCollection.title
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.displayName
        is OfLists -> list.name
        is OfStarterPacks -> starterPack.name
    }

private val ProfileCollection.description
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.displayName
        is OfLists -> list.name
        is OfStarterPacks -> starterPack.name
    }

private val ProfileCollection.avatar
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.avatar
        is OfLists -> list.avatar
        is OfStarterPacks -> null
    }

private val ProfileCollection.uriPath
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.uri.path
        is OfLists -> list.uri.path
        is OfStarterPacks -> starterPack.uri.path
    }

private val ProfileCollection.shape: RoundedPolygonShape.Custom
    get() = when (this) {
        is OfFeedGenerators -> FeedGeneratorCollectionShape
        is OfLists -> ListCollectionShape
        is OfStarterPacks -> StarterPackCollectionShape
    }

private val ProfileCollection.avatarSharedElementKey: String
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.avatarSharedElementKey(
            ProfileCollectionSharedElementPrefix
        )

        is OfLists -> list.avatarSharedElementKey(
            ProfileCollectionSharedElementPrefix
        )

        is OfStarterPacks -> starterPack.avatarSharedElementKey(
            ProfileCollectionSharedElementPrefix
        )
    }

private val ProfileCollection.sharedElementType: Any
    get() = when (this) {
        is OfFeedGenerators -> feedGenerator.uri
        is OfLists -> list.uri
        is OfStarterPacks -> starterPack.uri
    }

private const val ProfileCollectionSharedElementPrefix = "profile-collection"