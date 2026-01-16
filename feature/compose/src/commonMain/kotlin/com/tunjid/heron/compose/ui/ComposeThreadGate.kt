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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.timeline.ui.post.PostInteractionStatus
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberUpdatedThreadGateSheetState

@Composable
fun ComposeThreadGate(
    modifier: Modifier = Modifier,
    interactionSettingsPreference: PostInteractionSettingsPreference?,
    onInteractionSettingsUpdated: (PostInteractionSettingsPreference) -> Unit,
) {
    val threadGateSheetState = rememberUpdatedThreadGateSheetState(
        onDefaultThreadGateUpdated = onInteractionSettingsUpdated,
    )

    ElevatedCard(
        modifier = modifier,
        shape = CircleShape,
        onClick = {
            threadGateSheetState.show(interactionSettingsPreference)
        },
    ) {
        PostInteractionStatus(
            modifier = Modifier
                .padding(
                    vertical = 4.dp,
                    horizontal = 8.dp,
                ),
            preference = interactionSettingsPreference,
        )
    }
}
