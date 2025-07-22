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

package com.tunjid.heron.messages

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Conversation
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.tiler.compose.PivotedTilingEffect
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf

@Composable
internal fun MessagesScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val items by rememberUpdatedState(state.tiledItems)

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(),
    ) {
        items(
            items = items,
            key = { it.id.id },
            itemContent = { conversation ->
                Conversation(
                    paneScaffoldState = paneScaffoldState,
                    signedInProfileId = state.signedInProfile?.did,
                    conversation = conversation,
                    onConversationClicked = {
                        actions(
                            Action.Navigate.DelegateTo(
                                NavigationAction.Common.ToConversation(
                                    id = conversation.id,
                                    members = conversation.members,
                                    sharedElementPrefix = conversation.id.id,
                                    referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                )
                            )
                        )
                    }
                )
            }
        )
    }

    listState.PivotedTilingEffect(
        items = items,
        onQueryChanged = { query ->
            actions(
                Action.Tile(
                    tilingAction = TilingState.Action.LoadAround(
                        query = query ?: state.tilingData.currentQuery
                    )
                )
            )
        }
    )
}

@Composable
fun Conversation(
    paneScaffoldState: PaneScaffoldState,
    signedInProfileId: ProfileId?,
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onConversationClicked: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(
                horizontal = 8.dp,
            )
            .clickable {
                onConversationClicked()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val participants = remember(
            signedInProfileId,
            conversation.members,
        ) {
            conversation.members
                .filter { it.did != signedInProfileId }
                .take(3)
        }
        ConversationMembers(
            paneScaffoldState = paneScaffoldState,
            members = participants,
            conversationId = conversation.id,
        )
        Column {
            Text(
                text = conversation.lastMessage?.text ?: "",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ConversationMembers(
    paneScaffoldState: PaneScaffoldState,
    conversationId: ConversationId,
    members: List<Profile>,
) = with(paneScaffoldState) {
    FlowRow(
        modifier = Modifier
            .width(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
    ) {
        val membersSize = members.size
        members.forEachIndexed { index, profile ->
            updatedMovableStickySharedElementOf(
                sharedContentState = paneScaffoldState.rememberSharedContentState(
                    key = "$conversationId-${profile.did}"
                ),
                state = remember(profile.did) {
                    ImageArgs(
                        url = profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(
                        if (membersSize == 1) 44.dp else 28.dp
                    )
                    .offset(
                        x = if (membersSize == 2 && index == 1) -(4).dp else 0.dp,
                        y = if (membersSize == 2) when (index) {
                            0 -> (-7).dp
                            else -> 7.dp
                        }
                        else 0.dp,
                    )
                    .clip(RoundedCornerShape(28.dp)),
                sharedElement = { args, innerModifier ->
                    AsyncImage(args, innerModifier)
                }
            )
        }
    }
}
