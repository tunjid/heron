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

package com.tunjid.heron.messages.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape

@Composable
internal fun ConversationSearchResults(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    autoCompletedProfiles: List<ProfileWithViewerState>,
) = with(paneScaffoldState) {
    val topClearance = UiTokens.statusBarHeight + UiTokens.toolbarHeight + 8.dp
    val tabsOffsetNestedScrollConnection = rememberAccumulatedOffsetNestedScrollConnection(
        maxOffset = { Offset.Zero },
        minOffset = { Offset(x = 0f, y = -UiTokens.toolbarHeight.toPx()) },
    )

    ElevatedCard(
        modifier = modifier
            .nestedScroll(tabsOffsetNestedScrollConnection)
            .offset {
                IntOffset(
                    x = 0,
                    y = topClearance.roundToPx(),
                ) + tabsOffsetNestedScrollConnection.offset.round()
            },
    ) {
        LazyColumn {
            items(autoCompletedProfiles) { profileWithViewerState ->
                val profile = profileWithViewerState.profile
                AttributionLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                        }
                        .padding(16.dp),
                    avatar = {
                        PaneStickySharedElement(
                            modifier = Modifier
                                .size(36.dp),
                            sharedContentState = rememberSharedContentState(
                                key = "$ConversationSearchResult-${profile.did}",
                            ),
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .fillParentAxisIfFixedOrWrap()
                                    .clickable {
                                    },
                                args = remember(profile.avatar) {
                                    ImageArgs(
                                        url = profile.avatar?.uri,
                                        contentDescription = profile.displayName,
                                        contentScale = ContentScale.Crop,
                                        shape = RoundedPolygonShape.Circle,
                                    )
                                },
                            )
                        }
                    },
                    label = {
                        Text(
                            text = profile.displayName
                                ?: profile.handle.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )
            }
        }
    }
}

private const val ConversationSearchResult = "ConversationSearchResult"
