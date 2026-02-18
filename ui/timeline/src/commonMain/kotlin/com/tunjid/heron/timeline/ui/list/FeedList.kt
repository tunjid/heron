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

package com.tunjid.heron.timeline.ui.list

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.Timeline.Update
import com.tunjid.heron.data.core.types.ListUri
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.BlueskyClouds
import com.tunjid.heron.timeline.utilities.ListCollectionShape
import com.tunjid.heron.timeline.utilities.StarterPackCollectionShape
import com.tunjid.heron.timeline.utilities.TimelineStatusSelection
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.RecordLayout
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.list_by
import heron.ui.timeline.generated.resources.starter_pack_by
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedList(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    sharedElementPrefix: String,
    list: FeedList,
    status: Timeline.Home.Status?,
    onListStatusUpdated: (Update.OfList) -> Unit,
) =
    with(movableElementSharedTransitionScope) {
        RecordLayout(
            modifier = modifier,
            movableElementSharedTransitionScope = movableElementSharedTransitionScope,
            title = list.name,
            subtitle = stringResource(Res.string.list_by, list.creator.handle.id),
            description = list.description,
            blurb = null,
            sharedElementPrefix = sharedElementPrefix,
            sharedElementType = list.uri,
            avatar = {
                val avatar = list.avatar ?: BlueskyClouds
                PaneStickySharedElement(
                    modifier = Modifier.size(44.dp),
                    sharedContentState =
                        rememberSharedContentState(
                            key = list.avatarSharedElementKey(sharedElementPrefix)
                        ),
                ) {
                    AsyncImage(
                        modifier = Modifier.fillParentAxisIfFixedOrWrap(),
                        args =
                            remember(avatar) {
                                ImageArgs(
                                    url = avatar.uri,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                    shape = ListCollectionShape,
                                )
                            },
                    )
                }
            },
            action = {
                status?.let { currentStatus ->
                    FeedListStatus(
                        status = currentStatus,
                        uri = list.uri,
                        onListStatusUpdated = onListStatusUpdated,
                    )
                }
            },
        )
    }

@Composable
fun StarterPack(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    sharedElementPrefix: String,
    starterPack: StarterPack,
) =
    with(movableElementSharedTransitionScope) {
        RecordLayout(
            modifier = modifier,
            movableElementSharedTransitionScope = movableElementSharedTransitionScope,
            title = starterPack.name,
            subtitle = stringResource(Res.string.starter_pack_by, starterPack.creator.handle.id),
            description = starterPack.description,
            blurb = "",
            sharedElementPrefix = sharedElementPrefix,
            sharedElementType = starterPack.uri,
            avatar = {
                val avatar = BlueskyClouds
                PaneStickySharedElement(
                    modifier = Modifier.size(44.dp),
                    sharedContentState =
                        rememberSharedContentState(
                            key = starterPack.avatarSharedElementKey(sharedElementPrefix)
                        ),
                ) {
                    AsyncImage(
                        modifier = Modifier.fillParentAxisIfFixedOrWrap(),
                        args =
                            remember(avatar) {
                                ImageArgs(
                                    url = avatar.uri,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                    shape = StarterPackCollectionShape,
                                )
                            },
                    )
                }
            },
        )
    }

@Composable
fun FeedListStatus(
    status: Timeline.Home.Status,
    uri: ListUri,
    onListStatusUpdated: (Update.OfList) -> Unit,
) {
    TimelineStatusSelection(
        currentStatus = status,
        onStatusSelected = { selectedStatus ->
            val update =
                when (selectedStatus) {
                    Timeline.Home.Status.Pinned -> Update.OfList.Pin(uri)
                    Timeline.Home.Status.Saved -> Update.OfList.Save(uri)
                    Timeline.Home.Status.None -> Update.OfList.Remove(uri)
                }
            onListStatusUpdated(update)
        },
    )
}
