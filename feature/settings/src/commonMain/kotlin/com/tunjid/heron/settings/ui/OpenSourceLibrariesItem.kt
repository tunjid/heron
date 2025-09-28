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

package com.tunjid.heron.settings.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.collapse_icon
import heron.feature.settings.generated.resources.expand_icon
import heron.feature.settings.generated.resources.open_source_licenses
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OpenSourceLibrariesItem(
    libraries: Libs?,
) {
    var showLibraries by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showLibraries = !showLibraries },
    ) {
        SettingsItemRow(
            modifier = Modifier
                .fillMaxWidth(),
            title = stringResource(Res.string.open_source_licenses),
        ) {
            val iconRotation = animateFloatAsState(
                targetValue = if (showLibraries) 0f
                else 180f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            Icon(
                modifier = Modifier.graphicsLayer {
                    rotationX = iconRotation.value
                },
                imageVector = Icons.Default.ExpandLess,
                contentDescription = stringResource(
                    if (showLibraries) Res.string.collapse_icon
                    else Res.string.expand_icon,
                ),
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showLibraries,
            enter = EnterTransition,
            exit = ExitTransition,
            content = {
                LibrariesContainer(
                    libraries = libraries,
                    textStyles = LibraryDefaults.libraryTextStyles(
                        nameTextStyle = MaterialTheme.typography.bodyMedium,
                        authorTextStyle = MaterialTheme.typography.bodySmall,
                        licensesTextStyle = MaterialTheme.typography.bodySmall,
                    ),
                    modifier = Modifier
                        .padding(
                            top = 8.dp,
                            start = 8.dp,
                            end = 8.dp,
                        )
                        .fillMaxWidth(),
                )
            },
        )
    }
}

private val EnterTransition = fadeIn() + slideInVertically { -it }
private val ExitTransition =
    shrinkOut { IntSize(it.width, 0) } + slideOutVertically { -it } + fadeOut()
