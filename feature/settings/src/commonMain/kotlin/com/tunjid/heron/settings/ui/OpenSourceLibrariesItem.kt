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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LicenseDialogBody
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.close
import heron.feature.settings.generated.resources.collapse_icon
import heron.feature.settings.generated.resources.expand_icon
import heron.feature.settings.generated.resources.open_source_licenses
import heron.feature.settings.generated.resources.view_website
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpenSourceLibrariesItem(
    modifier: Modifier = Modifier,
    libraries: Libs?,
) {
    var showLibraries by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showLibraries = !showLibraries },
    ) {
        SettingsItemRow(
            modifier = Modifier
                .fillMaxWidth(),
            title = stringResource(Res.string.open_source_licenses),
            icon = Icons.Rounded.Copyright,
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
                LibrariesHorizontalGrid(
                    libraries = libraries,
                    modifier = Modifier
                        .height(200.dp)
                        .padding(
                            top = 8.dp,
                            start = 24.dp,
                            end = 24.dp,
                        )
                        .fillMaxWidth(),
                )
            },
        )
    }
}

@Composable
private fun LibrariesHorizontalGrid(
    modifier: Modifier = Modifier,
    libraries: Libs?,
) {
    val libs = libraries?.libraries
        ?.distinctBy(Library::name)
        ?: persistentListOf()
    val selectedLibrary = remember { mutableStateOf<Library?>(null) }

    LazyHorizontalGrid(
        modifier = modifier,
        rows = GridCells.Adaptive(56.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = libs,
            itemContent = { library ->
                Library(
                    library = library,
                    onLibraryClicked = selectedLibrary::value::set,
                )
            },
        )
    }

    val library = selectedLibrary.value
    if (library != null) {
        LicenseDialog(
            library = library,
            onDismiss = {
                selectedLibrary.value = null
            },
        )
    }
}

@Composable
private fun Library(
    library: Library,
    onLibraryClicked: (Library) -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable {
                val license = library.licenses.firstOrNull()
                if (!license?.htmlReadyLicenseContent.isNullOrBlank()) {
                    onLibraryClicked(library)
                }
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = {
            Text(
                text = library.name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = library.author,
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                library.licenses.forEach {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
    )
}

@Composable
private fun LicenseDialog(
    library: Library,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            LicenseDialogBody(
                library = library,
                colors = LibraryDefaults.libraryColors(),
                modifier = Modifier,
            )
        },
        confirmButton = {
            library.website?.let { website ->
                TextButton(
                    onClick = {
                        runCatching { uriHandler.openUri(website) }
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.view_website),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    stringResource(Res.string.close),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
    )
}

private val EnterTransition = fadeIn() + slideInVertically { -it }
private val ExitTransition =
    shrinkOut { IntSize(it.width, 0) } + slideOutVertically { -it } + fadeOut()
