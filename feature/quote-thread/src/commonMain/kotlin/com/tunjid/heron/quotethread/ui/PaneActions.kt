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

package com.tunjid.heron.quotethread.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import com.tunjid.heron.quotethread.State
import com.tunjid.heron.sheets.rememberInferenceSheetState
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import heron.feature.quote_thread.generated.resources.Res
import heron.feature.quote_thread.generated.resources.spill_the_tea
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
) {
    val inferenceSheetState = rememberInferenceSheetState()
    // Only surface the tea when the device can actually run on-device inference.
    if (state.canRunInference) AppBarIconButton(
        icon = Icons.Rounded.AutoAwesome,
        iconDescription = stringResource(Res.string.spill_the_tea),
        onClick = onClick@{
            val anchorPost = state.anchorPost ?: return@onClick
            inferenceSheetState.spillTea(
                post = anchorPost,
            )
        },
    )
}
