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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.State
import com.tunjid.heron.compose.hasLongPost
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.automirrored.Send
import com.tunjid.heron.ui.scaffold.scaffold.PaneFab
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.links
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.post
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.TopAppBarFab(
    modifier: Modifier = Modifier,
    state: State,
    onCreatePost: (Action.CreatePost) -> Unit,
) {
    AppBarIconButton(
        modifier = modifier
            .run {
                if (isActive) sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(ComposePostFabSharedElementKey),
                    visible = state.hasLongPost,
                    zIndexInOverlay = UiTokens.fabSharedElementZIndex,
                )
                else this
            },
        icon = HeronIcons.AutoMirrored.Send,
        iconDescription = stringResource(Res.string.post),
        onClick = onClick@{
            state.createPostAction()?.let(onCreatePost)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            ComposeThreadGate(
                interactionSettingsPreference = state.interactionsPreference,
                onInteractionSettingsUpdated = {
                    onAction(Action.UpdateInteractionSettings(it))
                },
            )
            ComposePostLanguage(
                languages = state.postLanguages,
                onLanguagesChanged = {
                    onAction(Action.SetPostLanguages(it))
                },
            )
        }
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
    PaneFab(
        modifier = modifier
            .sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(ComposePostFabSharedElementKey),
                visible = !state.hasLongPost,
                zIndexInOverlay = UiTokens.fabSharedElementZIndex,
            )
            .alpha(if (state.postText.text.isNotBlank()) 1f else 0.6f),
        text = stringResource(Res.string.post),
        icon = HeronIcons.AutoMirrored.Send,
        expanded = state.fabExpanded,
        onClick = onClick@{
            state.createPostAction()?.let(onCreatePost)
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
        langs = postLanguages,
        media = video?.let(::listOf) ?: photos,
        embeddedRecordReference = embeddedRecord?.reference,
        linkPreview = linkPreview,
        interactionPreference = interactionsPreference,
        sourceDraftId = draftId,
    )
}

private object ComposePostFabSharedElementKey
