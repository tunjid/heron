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

package com.tunjid.heron.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.State
import com.tunjid.heron.compose.hasLongPost
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.text.links
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.post
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.TopAppBarFab(
    modifier: Modifier,
    state: State,
    onCreatePost: (Action.CreatePost) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    FloatingActionButton(
        modifier = modifier
            .height(36.dp)
            .run {
                if (isActive) sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(ComposePostFabSharedElementKey),
                    visible = state.hasLongPost,
                    zIndexInOverlay = UiTokens.fabSharedElementZIndex,
                )
                else this
            },
        shape = CircleShape,
        onClick = onClick@{
            state.createPostAction()?.let {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onCreatePost(it)
            }
        },
        content = {
            Text(
                text = stringResource(Res.string.post),
            )
        },
    )
}

@Composable
fun PaneScaffoldState.ComposePostFabRow(
    modifier: Modifier,
    state: State,
    onAction: (Action) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp),
        ) {
            ComposeThreadGate(
                interactionSettingsPreference = state.interactionsPreference,
                onInteractionSettingsUpdated = {
                    onAction(Action.UpdateInteractionSettings(it))
                },
                recentLists = state.recentLists,
                onRequestRecentLists = {
                    onAction(Action.UpdateRecentLists)
                },
            )
        }
        Spacer(Modifier.weight(1f))
        val show = if (inPredictiveBack) !state.hasLongPost else true
        if (show) ComposePostFab(
            state = state,
            onCreatePost = onAction,
        )
    }
}

@Composable
private fun PaneScaffoldState.ComposePostFab(
    modifier: Modifier = Modifier,
    state: State,
    onCreatePost: (Action.CreatePost) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    PaneFab(
        modifier = modifier
            .sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(ComposePostFabSharedElementKey),
                visible = !state.hasLongPost,
                zIndexInOverlay = UiTokens.fabSharedElementZIndex,
            )
            .alpha(if (state.postText.text.isNotBlank()) 1f else 0.6f),
        text = stringResource(Res.string.post),
        icon = Icons.AutoMirrored.Rounded.Send,
        expanded = state.fabExpanded,
        onClick = onClick@{
            state.createPostAction()?.let {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onCreatePost(it)
            }
        },
    )
}

private fun State.createPostAction(): Action.CreatePost? {
    val authorId = signedInProfile?.did ?: return null
    val postText = postText
    return Action.CreatePost(
        postType = postType,
        authorId = authorId,
        text = postText.text,
        links = postText.annotatedString.links(),
        media = video?.let(::listOf) ?: photos,
        embeddedRecordReference = embeddedRecord?.reference,
        interactionPreference = interactionsPreference,
    )
}

private object ComposePostFabSharedElementKey
