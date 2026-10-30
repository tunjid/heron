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

package com.tunjid.heron.timeline.ui.label

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.ui.RadioButton
import com.tunjid.heron.ui.rememberLatchedState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LabelSetting(
    enabled: Boolean,
    labelName: String,
    labelDescription: String?,
    selectedVisibility: Label.Visibility,
    visibilities: List<Label.Visibility>,
    modifier: Modifier = Modifier,
    visibilityStringResource: (Label.Visibility) -> StringResource,
    onVisibilityChanged: (Label.Visibility) -> Unit,
) {
    val latchedVisibilityState = rememberLatchedState(selectedVisibility)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = labelName,
            style = MaterialTheme.typography.bodySmallEmphasized,
        )
        labelDescription?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visibilities.forEach { visibility ->
                val selected = latchedVisibilityState.value == visibility
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = {
                                // A label setting cannot be unselected.
                                if (selected) return@selectable
                                latchedVisibilityState.latch(visibility)
                                onVisibilityChanged(visibility)
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        enabled = enabled,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(visibilityStringResource(visibility)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
