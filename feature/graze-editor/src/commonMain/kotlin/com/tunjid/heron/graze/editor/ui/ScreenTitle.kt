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

package com.tunjid.heron.graze.editor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.RecordKey
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.utilities.TimelineTitle
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.graze_editor
import heron.feature.graze_editor.generated.resources.no_display_name
import org.jetbrains.compose.resources.stringResource

sealed class Title {
    abstract val path: List<Int>

    data class Pending(
        override val path: List<Int>,
        val recordKey: RecordKey,
        val displayName: String?,
    ) : Title()

    data class Created(
        override val path: List<Int>,
        val sharedElementPrefix: String,
        val feedGenerator: FeedGenerator,
    ) : Title()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Title(
    modifier: Modifier = Modifier,
    title: Title,
    paneScaffoldState: PaneScaffoldState,
    onTitleClicked: () -> Unit,
) {
    AnimatedContent(
        modifier =
            modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onTitleClicked,
                ),
        targetState = title,
        transitionSpec = { TitleTransitionSpec },
        contentKey = { currentTitle -> currentTitle.transitionKey() },
    ) { currentTitle ->
        when (currentTitle) {
            is Title.Created ->
                TimelineTitle(
                    modifier = Modifier,
                    movableElementSharedTransitionScope = paneScaffoldState,
                    timeline =
                        remember(currentTitle.feedGenerator) {
                            Timeline.Home.Feed.stub(currentTitle.feedGenerator)
                        },
                    sharedElementPrefix = currentTitle.sharedElementPrefix,
                    hasUpdates = false,
                    onPresentationSelected = { _, _ -> },
                )
            is Title.Pending ->
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        modifier = Modifier,
                        text = stringResource(Res.string.graze_editor),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                    )
                    Text(
                        modifier = Modifier,
                        text =
                            currentTitle.displayName ?: stringResource(Res.string.no_display_name),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
        }
    }
}

private fun Title.transitionKey(): String =
    when (this) {
        is Title.Created -> "${feedGenerator.displayName}-${path.joinToString("created")}"
        is Title.Pending -> "${recordKey.value}-${path.joinToString("pending")}"
    }

private val TitleTransitionSpec = fadeIn() togetherWith fadeOut()
