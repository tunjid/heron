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
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.profile.ProfileHandle
import com.tunjid.heron.timeline.ui.profile.ProfileName
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.treenav.compose.UpdatedMovableSharedElementOf
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf

@Composable
internal fun ConversationTitle(
    sharedElementPrefix: String,
    signedInProfileId: ProfileId?,
    participants: List<Profile>,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile) -> Unit,
) {
    val hasMultipleParticipants =
        remember(signedInProfileId, participants.size) {
            participants.filter { it.did != signedInProfileId }.size > 1
        }
    if (hasMultipleParticipants) {
        MultipleParticipantTitle(
            sharedElementPrefix = sharedElementPrefix,
            participants = participants,
            paneScaffoldState = paneScaffoldState,
            onProfileClicked = onProfileClicked,
        )
    } else {
        SingleMemberTitle(
            signedInProfileId = signedInProfileId,
            sharedElementPrefix = sharedElementPrefix,
            participants = participants,
            paneScaffoldState = paneScaffoldState,
            onProfileClicked = onProfileClicked,
        )
    }
}

@Composable
private fun MultipleParticipantTitle(
    sharedElementPrefix: String,
    participants: List<Profile>,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile) -> Unit,
) =
    with(paneScaffoldState) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            participants.forEachIndexed { index, participant ->
                UpdatedMovableSharedElementOf(
                    sharedContentState =
                        rememberSharedContentState(
                            key = participant.avatarSharedElementKey(prefix = sharedElementPrefix)
                        ),
                    state =
                        remember(participant.avatar?.uri) {
                            ImageArgs(
                                url = participant.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                shape = RoundedPolygonShape.Circle,
                                contentDescription = null,
                            )
                        },
                    zIndexInOverlay = UiTokens.higherThanAppBarSharedElementZIndex(),
                    modifier =
                        Modifier.size(32.dp).offset(x = index * (-8).dp).clickable {
                            onProfileClicked(participant)
                        },
                    sharedElement = { args, innerModifier -> AsyncImage(args, innerModifier) },
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(modifier = Modifier, text = "roomName")
        }
    }

@Composable
private fun SingleMemberTitle(
    signedInProfileId: ProfileId?,
    sharedElementPrefix: String,
    participants: List<Profile>,
    paneScaffoldState: PaneScaffoldState,
    onProfileClicked: (Profile) -> Unit,
) =
    with(paneScaffoldState) {
        val profile = participants.firstOrNull { it.did != signedInProfileId } ?: return
        AttributionLayout(
            modifier = Modifier.clickable { onProfileClicked(profile) },
            avatar = {
                UpdatedMovableStickySharedElementOf(
                    modifier =
                        Modifier.size(UiTokens.avatarSize).clickable { onProfileClicked(profile) },
                    sharedContentState =
                        rememberSharedContentState(
                            key = profile.avatarSharedElementKey(prefix = sharedElementPrefix)
                        ),
                    zIndexInOverlay = UiTokens.higherThanAppBarSharedElementZIndex(),
                    state =
                        remember(profile.avatar) {
                            ImageArgs(
                                url = profile.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = profile.contentDescription,
                                shape = RoundedPolygonShape.Circle,
                            )
                        },
                    sharedElement = { state, modifier -> AsyncImage(state, modifier) },
                )
            },
            label = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProfileName(profile = profile)
                    ProfileHandle(profile = profile)
                }
            },
        )
    }
