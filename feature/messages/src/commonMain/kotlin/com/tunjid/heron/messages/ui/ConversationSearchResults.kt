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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.composables.accumulatedoffsetnestedscrollconnection.rememberAccumulatedOffsetNestedScrollConnection
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.ProfileWithViewerState
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.messages.ConversationSearchResult
import com.tunjid.heron.messages.canBeMessaged
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import heron.feature.messages.generated.resources.Res
import heron.feature.messages.generated.resources.error_cannot_be_messaged
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConversationSearchResults(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    autoCompletedProfiles: List<ProfileWithViewerState>,
    onProfileClicked: (Profile) -> Unit,
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
                val canMessage = profileWithViewerState.canBeMessaged()
                AttributionLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (canMessage) 1f else 0.6f)
                        .clickable {
                            onProfileClicked(profile)
                        }
                        .padding(16.dp),
                    avatar = {
                        PaneStickySharedElement(
                            modifier = Modifier
                                .size(36.dp),
                            sharedContentState = rememberSharedContentState(
                                key = profile.avatarSharedElementKey(ConversationSearchResult),
                            ),
                        ) {
                            AsyncImage(
                                modifier = Modifier
                                    .fillParentAxisIfFixedOrWrap(),
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
                        Column {
                            ProfileName(profile = profile)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfileHandle(profile = profile)
                                if (!canMessage) {
                                    Text(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp),
                                        text = Bullet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    Text(
                                        text = stringResource(Res.string.error_cannot_be_messaged),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

private const val Bullet = "•"
