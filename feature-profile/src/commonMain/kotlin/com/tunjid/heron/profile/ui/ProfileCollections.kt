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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.profile.Action
import com.tunjid.heron.profile.ProfileCollection
import com.tunjid.heron.profile.ProfileCollectionStateHolder
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
internal fun ProfileCollection(
    collectionStateHolder: ProfileCollectionStateHolder,
    actions: (Action) -> Unit,
) {
    val listState = rememberLazyListState()
    val collectionState by collectionStateHolder.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
    ) {
        when (val state = collectionState) {
            is ProfileCollection.OfFeedGenerators -> items(
                items = state.feedGenerators,
                key = { it.cid.id },
                itemContent = {
                    AttributionLayout(
                        modifier = Modifier.fillMaxWidth(),
                        avatar = {
                            AsyncImage(
                                modifier = Modifier
                                    .size(36.dp),
                                args = remember(it.avatar) {
                                    ImageArgs(
                                        url = it.avatar?.uri,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedPolygonShape.Circle,
                                    )
                                },
                            )
                        },
                        label = {
                            Text(text = it.displayName)
                        },
                    )
                }
            )

            is ProfileCollection.OfLists -> items(
                items = state.items,
                key = { it.cid.id },
                itemContent = {
                    AttributionLayout(
                        modifier = Modifier.fillMaxWidth(),
                        avatar = {
                            AsyncImage(
                                modifier = Modifier
                                    .size(36.dp),
                                args = remember(it.avatar) {
                                    ImageArgs(
                                        url = it.avatar?.uri,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedPolygonShape.Circle,
                                    )
                                },
                            )
                        },
                        label = {
                            Text(text = it.name)
                        },
                    )
                }
            )

            is ProfileCollection.OfStarterPacks -> items(
                items = state.starterPacks,
                key = { it.cid.id },
                itemContent = {
                    AttributionLayout(
                        modifier = Modifier.fillMaxWidth(),
                        avatar = {
                            AsyncImage(
                                modifier = Modifier
                                    .size(36.dp),
                                args = remember(Unit) {
                                    ImageArgs(
                                        url = it.creator.avatar?.uri,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedPolygonShape.Circle,
                                    )
                                },
                            )
                        },
                        label = {
                            Text(text = it.name)
                        },
                    )
                }
            )
        }
    }
}