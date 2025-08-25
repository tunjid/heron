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

package com.tunjid.heron.conversation.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.contentDescription
import com.tunjid.heron.data.core.types.ConversationId
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableSharedElementOf
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ConversationTitle(
    conversationId: ConversationId,
    signedInProfileId: ProfileId?,
    participants: List<Profile>,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile) -> Unit,
) {
    val hasMultipleParticipants = remember(
        signedInProfileId,
        participants.size
    ) {
        participants.filter { it.did != signedInProfileId }.size > 1
    }
    if (hasMultipleParticipants) {
        MultipleParticipantTitle(
            conversationId = conversationId,
            participants = participants,
            paneScaffoldState = paneScaffoldState,
            onProfileClicked = onProfileClicked,
        )
    } else {
        SingleMemberTitle(
            participants = participants,
            signedInProfileId = signedInProfileId,
            conversationId = conversationId,
            paneScaffoldState = paneScaffoldState,
            onProfileClicked = onProfileClicked,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MultipleParticipantTitle(
    conversationId: ConversationId,
    participants: List<Profile>,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile) -> Unit,
) = with(paneScaffoldState) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        participants.forEachIndexed { index, participant ->
            updatedMovableSharedElementOf(
                sharedContentState = rememberSharedContentState(
                    key = participant.conversationSharedElementKey(conversationId)
                ),
                state = remember(participant.avatar?.uri) {
                    ImageArgs(
                        url = participant.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        shape = RoundedPolygonShape.Circle,
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = index * (-8).dp)
                    .clickable { onProfileClicked(participant) },
                sharedElement = { args, innerModifier ->
                    AsyncImage(args, innerModifier)
                }
            )
        }
        Spacer(
            modifier = Modifier
                .width(16.dp)
        )
        Text(
            modifier = Modifier,
            text = "roomName",
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SingleMemberTitle(
    signedInProfileId: ProfileId?,
    conversationId: ConversationId,
    participants: List<Profile>,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile) -> Unit,
) = with(paneScaffoldState) {
    val profile = participants.firstOrNull { it.did != signedInProfileId } ?: return
    AttributionLayout(
        modifier = Modifier
            .clickable { onProfileClicked(profile) },
        avatar = {
            updatedMovableStickySharedElementOf(
                modifier = Modifier
                    .size(UiTokens.avatarSize)
                    .clickable { onProfileClicked(profile) },
                sharedContentState = rememberSharedContentState(
                    key = profile.conversationSharedElementKey(conversationId),
                ),
                state = remember(profile.avatar) {
                    ImageArgs(
                        url = profile.avatar?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = profile.contentDescription,
                        shape = RoundedPolygonShape.Circle,
                    )
                },
                sharedElement = { state, modifier ->
                    AsyncImage(state, modifier)
                }
            )
        },
        label = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProfileName(
                    profile = profile
                )
                ProfileHandle(
                    profile = profile
                )
            }
        },
    )
}

internal fun Profile.conversationSharedElementKey(
    conversationId: ConversationId
): String = "${conversationId.id}-${did}"