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

package com.tunjid.heron.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import kotlinx.coroutines.delay

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun MatteControlsLightPreview() {
    MaterialTheme(
        colorScheme = lightColorScheme(),
    ) {
        MatteControls()
    }
}

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun MatteControlsDarkPreview() {
    MaterialTheme(
        colorScheme = darkColorScheme(),
    ) {
        MatteControls()
    }
}

@Composable
private fun MatteControls() {
    Surface {
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewHeading(
                text = "Selector",
            )
            var selectedItem by remember { mutableStateOf(PreviewItems.first()) }
            Selector(
                modifier = Modifier
                    .fillMaxWidth(),
                state = rememberSelectorState(
                    selectedItem = selectedItem,
                    items = PreviewItems,
                ),
                onItemSelected = { selectedItem = it },
            ) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = item == selectedItem,
                            onClick = { selectedItem = item },
                            role = Role.RadioButton,
                        )
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item,
                    )
                }
            }

            PreviewHeading(
                text = "Switches",
            )
            PreviewSwitches.forEachIndexed { index, label ->
                var checked by remember { mutableStateOf(index % 2 == 0) }
                PreviewRow(
                    label = label,
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                    )
                }
            }
            PreviewRow(
                label = "Disabled",
            ) {
                Switch(
                    checked = true,
                    onCheckedChange = null,
                    enabled = false,
                )
                Switch(
                    checked = false,
                    onCheckedChange = null,
                    enabled = false,
                )
            }

            PreviewHeading(
                text = "Checkboxes",
            )
            PreviewCheckboxes.forEachIndexed { index, label ->
                var checked by remember { mutableStateOf(index % 2 == 0) }
                PreviewRow(
                    label = label,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                    )
                }
            }
            PreviewRow(
                label = "Disabled",
            ) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    enabled = false,
                )
                Checkbox(
                    checked = false,
                    onCheckedChange = null,
                    enabled = false,
                )
            }

            PreviewHeading(
                text = "Slider",
            )
            var value by remember { mutableFloatStateOf(0.3f) }
            Slider(
                value = value,
                onValueChange = { value = it },
            )

            PreviewHeading(
                text = "Playing, with the odd jump",
            )
            var playback by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(Unit) {
                val random = Random(PreviewPlaybackSeed)
                var elapsedMs = 0L
                while (true) {
                    @Suppress("ConvertLongToDuration")
                    delay(PreviewPlaybackTickMs)
                    elapsedMs += PreviewPlaybackTickMs
                    playback = when {
                        elapsedMs % PreviewPlaybackJumpMs == 0L -> 0.1f + (random.nextFloat() * 0.8f)
                        playback >= 1f -> 0f
                        else -> playback + PreviewPlaybackStep
                    }
                }
            }
            Slider(
                value = playback,
                onValueChange = { playback = it },
            )
            Slider(
                value = 0.6f,
                onValueChange = {},
                enabled = false,
            )
        }
    }
}

@Composable
private fun PreviewHeading(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PreviewRow(
    label: String,
    controls: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = label,
        )
        controls()
    }
}

private val PreviewItems = listOf(
    "System default",
    "Light",
    "Dark",
    "Black Heron",
    "Pied Heron",
)

private val PreviewSwitches = listOf(
    "Autoplay GIFs",
    "Autoplay videos",
    "Show link previews",
)

private val PreviewCheckboxes = listOf(
    "Mentioned users",
    "Users you follow",
    "Your followers",
)

private const val PreviewPlaybackSeed = 7
private const val PreviewPlaybackJumpMs = 3_200L
private const val PreviewPlaybackTickMs = 16L
private const val PreviewPlaybackStep = 0.0015f
