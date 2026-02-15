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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.scaffold.scaffold.AppBarTitle
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.utilities.TimelineTitle
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.graze_editor
import heron.feature.graze_editor.generated.resources.graze_editor_level
import org.jetbrains.compose.resources.stringResource

sealed class Title {
    abstract val path: List<Int>

    data class Pending(
        override val path: List<Int>,
    ) : Title()

    data class Created(
        override val path: List<Int>,
        val feedGenerator: FeedGenerator,
    ) : Title()
}

@Composable
fun Title(
    modifier: Modifier = Modifier,
    title: Title,
    paneScaffoldState: PaneScaffoldState,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = title,
        transitionSpec = {
            TitleTransitionSpec
        },
        contentKey = { currentTitle ->
            when (currentTitle) {
                is Title.Created -> currentTitle.path.joinToString("created")
                is Title.Pending -> currentTitle.path.joinToString("pending")
            }
        },
    ) { currentTitle ->
        when (currentTitle) {
            is Title.Created -> TimelineTitle(
                modifier = Modifier,
                movableElementSharedTransitionScope = paneScaffoldState,
                timeline = remember {
                    Timeline.Home.Feed.stub(currentTitle.feedGenerator)
                },
                sharedElementPrefix = "",
                hasUpdates = false,
                onPresentationSelected = { _, _ -> },
            )
            is Title.Pending -> AppBarTitle(
                modifier = Modifier,
                title =
                if (currentTitle.path.isEmpty()) stringResource(
                    Res.string.graze_editor,
                )
                else stringResource(
                    Res.string.graze_editor_level,
                    currentTitle.path.size,
                ),
            )
        }
    }
}

private val TitleTransitionSpec = fadeIn() togetherWith fadeOut()
