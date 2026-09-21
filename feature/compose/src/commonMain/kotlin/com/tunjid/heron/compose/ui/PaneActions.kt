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

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.compose.Action
import com.tunjid.heron.compose.State
import com.tunjid.heron.compose.canDraft
import com.tunjid.heron.compose.drafts.rememberDraftsSheetState
import com.tunjid.heron.compose.hasLongPost
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Drafts
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.drafts
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
    actions: (Action) -> Unit,
) {
    val draftsSheetState = rememberDraftsSheetState(
        onDraftSelected = { actions(Action.LoadDraft(it)) },
    )
    if (state.canDraft) AppBarIconButton(
        modifier = Modifier
            .animateBounds(
                lookaheadScope = this@PaneActions,
                boundsTransform = childBoundsTransform,
            ),
        icon = HeronIcons.Regular.Drafts,
        iconDescription = stringResource(Res.string.drafts),
        onClick = draftsSheetState::showDrafts,
    )
    Box(
        modifier = Modifier
            .ifTrue(state.hasLongPost) {
                padding(horizontal = 8.dp)
            }
            .ifTrue(!state.hasLongPost) {
                // Always has to be in composition, so make very narrow
                requiredWidth(Dp.Hairline)
            },
    ) {
        TopAppBarFab(
            state = state,
            onCreatePost = actions,
        )
    }
}
