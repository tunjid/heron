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

package com.tunjid.heron.ui.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tunjid.heron.ui.modifiers.HaloState.Companion.rememberHaloState

/**
 * Both [HaloMode]s haloing a rounded card. `repeatable = true` keeps the effect looping so it stays
 * visible; run it in interactive/animated preview to watch the gradient travel the outline.
 */
@Preview
@Composable
private fun HaloPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.padding(40.dp),
                verticalArrangement = Arrangement.spacedBy(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HaloMode.entries.forEach { mode ->
                    val shape = remember { RoundedCornerShape(20.dp) }
                    val haloState = rememberHaloState(
                        shape = shape,
                        mode = mode,
                        repeatable = true,
                        width = 4.dp,
                    )
                    LaunchedEffect(haloState) {
                        haloState.highlight()
                    }
                    Box(
                        modifier = Modifier
                            .size(
                                width = 200.dp,
                                height = 72.dp,
                            )
                            .halo(haloState)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = mode.name)
                    }
                }
            }
        }
    }
}
