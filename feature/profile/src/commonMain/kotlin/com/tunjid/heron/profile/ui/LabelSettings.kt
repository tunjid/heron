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

package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.profile.LabelerSettingsStateHolder
import com.tunjid.heron.timeline.ui.label.locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LabelerSettings(
    modifier: Modifier = Modifier,
    stateHolder: LabelerSettingsStateHolder,
) {
    val state by stateHolder.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        items(
            items = state.labelSettings,
            itemContent = { labelSetting ->
                Column {
                    Column(
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val languageTag = Locale.current.toLanguageTag()
                        val locale = labelSetting.definition.locale(languageTag)
                        Text(
                            text = locale?.name ?: labelSetting.definition.identifier.value,
                            style = MaterialTheme.typography.bodySmallEmphasized,
                        )
                        locale?.description?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            Label.Visibility.all.forEachIndexed { index, visibility ->
                                val overrideColors = ButtonDefaults.filledTonalButtonColors()
                                ToggleButton(
                                    checked = labelSetting.visibility == visibility,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) stateHolder.accept(
                                            labelSetting.copy(visibility = visibility)
                                        )
                                        // Do nothing, cannot uncheck a label setting
                                        else Unit
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            role = Role.RadioButton
                                        },
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        checkedContainerColor = overrideColors.containerColor,
                                        checkedContentColor = overrideColors.contentColor,
                                    ),
                                    shapes = when (index) {
                                        0 -> LeadingButtonShape
                                        Label.Visibility.all.lastIndex -> TrailingButtonShape
                                        else -> MiddleButtonShape
                                    },
                                ) {
                                    Text(visibility.value)
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val LeadingButtonShape = RoundedCornerShape(
    topStart = 8.dp,
    topEnd = 0.dp,
    bottomStart = 8.dp,
    bottomEnd = 0.dp,
).let { ToggleButtonShapes(it, it, it) }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val MiddleButtonShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
).let { ToggleButtonShapes(it, it, it) }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val TrailingButtonShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 8.dp,
    bottomStart = 0.dp,
    bottomEnd = 8.dp,
).let { ToggleButtonShapes(it, it, it) }
