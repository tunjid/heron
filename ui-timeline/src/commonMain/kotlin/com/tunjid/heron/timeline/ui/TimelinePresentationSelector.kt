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

package com.tunjid.heron.timeline.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.timeline.utilities.icon
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.condensed_media
import heron.ui_timeline.generated.resources.expanded_media
import heron.ui_timeline.generated.resources.text_and_embeds
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TimelinePresentationSelector(
    modifier: Modifier = Modifier,
    alwaysExpanded: Boolean = false,
    selected: Timeline.Presentation,
    available: List<Timeline.Presentation>,
    onPresentationSelected: (Timeline.Presentation) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(
            if (alwaysExpanded) selected
            else null
        )
    }
    LookaheadScope {
        ElevatedCard(
            modifier = modifier,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.animateBounds(
                    lookaheadScope = this@LookaheadScope,
                ),
                horizontalArrangement = Arrangement.aligned(Alignment.End)
            ) {
                if (available.size > 1) available.forEach { presentation ->
                    val isSelected = selected == presentation
                    key(presentation.key) {
                        AnimatedVisibility(
                            modifier = Modifier.animateBounds(
                                lookaheadScope = this@LookaheadScope,
                            ),
                            visible = isSelected || expanded != null,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                        ) {
                            IconButton(
                                modifier = Modifier
                                    .size(40.dp),
                                onClick = {
                                    when (expanded) {
                                        null -> expanded = presentation
                                        presentation -> if (!alwaysExpanded) expanded = null
                                        else -> onPresentationSelected(presentation)
                                    }
                                },
                                content = {
                                    Icon(
                                        imageVector = presentation.icon,
                                        contentDescription = stringResource(
                                            when (presentation) {
                                                Timeline.Presentation.Text.WithEmbed -> Res.string.text_and_embeds
                                                Timeline.Presentation.Media.Condensed -> Res.string.condensed_media
                                                Timeline.Presentation.Media.Expanded -> Res.string.expanded_media
                                            }
                                        ),
                                        tint =
                                            if (presentation == selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(expanded, selected, alwaysExpanded) {
        if (!alwaysExpanded) {
            if (expanded != null && expanded != selected) {
                expanded = null
            }
        }
        onDispose { }
    }
}