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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.MorphingLoaderState.Companion.rememberMorphingLoaderState

/**
 * Each [LoaderCurve] across a spread of energies. Run it in interactive/animated preview to watch the
 * figure morph — the shape animates on its own clocks, while energy sets how busy it is.
 */
@Preview
@Composable
private fun MorphingLoaderPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LoaderCurve.entries.forEach { curve ->
                    Text(
                        text = curve.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PreviewEnergies.forEach { energy ->
                            val state = rememberMorphingLoaderState(
                                curve = curve,
                                energy = energy,
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                MorphingLoader(
                                    state = state,
                                    modifier = Modifier
                                        .size(56.dp),
                                )
                                Text(
                                    text = "${(energy * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The hidden-trace "firefly" mode: with `traceVisible = false` only the comet shows, running an
 * unseen — and still morphing — path. Interactive/animated preview recommended.
 */
@Preview
@Composable
private fun MorphingLoaderHiddenTracePreview() {
    MaterialTheme {
        Surface {
            Row(
                modifier = Modifier
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviewEnergies.forEach { energy ->
                    val state = rememberMorphingLoaderState(
                        energy = energy,
                        cometLength = 0.35f,
                        traceVisible = false,
                    )
                    MorphingLoader(
                        state = state,
                        modifier = Modifier
                            .size(72.dp),
                    )
                }
            }
        }
    }
}

private val PreviewEnergies = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
