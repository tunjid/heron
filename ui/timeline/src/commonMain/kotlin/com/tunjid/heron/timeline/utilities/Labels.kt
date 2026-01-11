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

package com.tunjid.heron.timeline.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.dismiss
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.label_source
import heron.ui.timeline.generated.resources.view_labeler
import org.jetbrains.compose.resources.stringResource

@Composable
inline fun LabelFlowRow(
    modifier: Modifier = Modifier,
    crossinline content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = {
            content()
        },
    )
}

@Composable
inline fun Label(
    modifier: Modifier = Modifier,
    contentDescription: String,
    isElevated: Boolean = false,
    crossinline icon: @Composable () -> Unit,
    crossinline description: @Composable () -> Unit,
    crossinline onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .background(
                color =
                if (isElevated) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                else Color.Transparent,
                shape = CircleShape,
            )
            .ifTrue(isElevated) {
                // Add padding bc of the background
                padding(
                    horizontal = 6.dp,
                    vertical = 2.dp,
                )
            }
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            }
            .clip(CircleShape)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        description()
    }
}

@Composable
fun LabelText(
    text: String,
) {
    Text(
        text = text,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

fun Label.Definition.locale(
    currentLanguageTag: String,
) = locales.list
    .firstOrNull { it.lang == currentLanguageTag }
    ?: locales.list
        .firstOrNull()

internal val LabelIconSize = 12.dp
