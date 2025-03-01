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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.cancel
import heron.ui_timeline.generated.resources.quote
import heron.ui_timeline.generated.resources.repost
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource


@Stable
class PostInteractionState private constructor(){
    var currentInteraction by mutableStateOf<Post.Interaction?>(null)
        internal set

    fun onInteraction(interaction: Post.Interaction) {
        currentInteraction = interaction
    }

    companion object {
        @Composable
        fun rememberPostInteractionState() = remember {
            PostInteractionState()
        }
    }
}

@Composable
fun PostInteractions(
    state: PostInteractionState,
    onInteractionConfirmed: (Post.Interaction) -> Unit,
    onQuotePostClicked: () -> Unit,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val hideSheet = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
                state.currentInteraction = null
            }
        }
    }

    LaunchedEffect(state.currentInteraction) {
        when (val interaction = state.currentInteraction) {
            null -> Unit
            is Post.Interaction.Create.Repost -> showBottomSheet = true
            is Post.Interaction.Create.Like,
            is Post.Interaction.Delete.RemoveRepost,
            is Post.Interaction.Delete.Unlike,
                -> {
                onInteractionConfirmed(interaction)
                state.currentInteraction = null
            }
        }
    }

    if (showBottomSheet) ModalBottomSheet(
        onDismissRequest = {
            showBottomSheet = false
        },
        sheetState = sheetState,
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                verticalArrangement = spacedBy(8.dp)
            ) {
                repeat(2) { index ->

                    val contentDescription = stringResource(
                        if (index == 0) Res.string.repost
                        else Res.string.quote
                    ).capitalize(locale = Locale.current)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .clickable {
                                if (index == 0) state.currentInteraction?.let(
                                    onInteractionConfirmed
                                )
                                else onQuotePostClicked()
                                hideSheet()
                            }
                            .padding(
                                horizontal = 8.dp,
                                vertical = 8.dp,
                            )
                            .semantics {
                                this.contentDescription = contentDescription
                            },
                        horizontalArrangement = spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Icon(
                                modifier = Modifier
                                    .size(24.dp),
                                imageVector = if (index == 0) Icons.Rounded.Repeat
                                else Icons.Rounded.FormatQuote,
                                contentDescription = null
                            )
                            Text(
                                modifier = Modifier,
                                text = contentDescription,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }

                // Sheet content
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { hideSheet() },
                    content = {
                        Text(
                            text = stringResource(Res.string.cancel)
                                .capitalize(Locale.current),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                )
                Spacer(
                    Modifier.height(16.dp)
                )
            }
        }
    )
}