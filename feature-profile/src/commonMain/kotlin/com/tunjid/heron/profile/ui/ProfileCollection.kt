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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tunjid.heron.ui.CollectionLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature_profile.generated.resources.Res
import heron.feature_profile.generated.resources.collection_by
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileCollection(
    collectionStateHolder: ProfileCollectionStateHolder,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val collectionState by collectionStateHolder.state.collectAsStateWithLifecycle()
    val updatedItems by rememberUpdatedState(collectionState.items)

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
                    modifier = Modifier.fillMaxWidth(),
                    title = collectionState.stringResource,
                    collection = profileCollection,
                    onCollectionClicked = { collection ->
                        actions(
                            Action.Navigate.DelegateTo(
                                NavigationAction.Common.ToRawUrl(
                                    path = collection.uriPath,
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
                query ?: collectionState.currentQuery
            )
        }
    )
}

@Composable
private fun ProfileCollection(
    modifier: Modifier = Modifier,
    title: StringResource,
    collection: ProfileCollection,
    onCollectionClicked: (ProfileCollection) -> Unit,
) {
    CollectionLayout(
        modifier = modifier
            .padding(
                vertical = 4.dp,
                horizontal = 24.dp
            ),
        title = collection.title,
        subtitle = stringResource(
            Res.string.collection_by,
            stringResource(title),
            collection.creator.handle.id,
        ),
        description = collection.description,
        blurb = "",
        avatar = {
            when (val avatar = collection.avatar) {
                null -> Icon(
                    modifier = Modifier
                        .size(44.dp),
                    imageVector = Icons.Rounded.RssFeed,
                    contentDescription = null,
                )

                else -> AsyncImage(
                    modifier = Modifier
                        .size(44.dp),
                    args = ImageArgs(
                        url = avatar.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = RoundedPolygonShape.Circle,
                    )
                )
            }
        },
        onClicked = {
            onCollectionClicked(collection)
        },
    )
}

//stringResource(
//Res.string.liked_by,
//format(feedGenerator.likeCount ?: 0L)
//)

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