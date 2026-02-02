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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LicenseDialogBody
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.close
import heron.feature.settings.generated.resources.open_source_licenses
import heron.feature.settings.generated.resources.view_website
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpenSourceLibrariesItem(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    libraries: Libs?,
) {
    ExpandableSettingsItemRow(
        modifier = modifier
            .fillMaxWidth(),
        title = stringResource(Res.string.open_source_licenses),
        icon = Icons.Rounded.Copyright,
        enabled = enabled,
    ) {
        LibrariesHorizontalGrid(
            libraries = libraries,
            modifier = Modifier
                .height(200.dp)
                .padding(
                    top = 8.dp,
                )
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun LibrariesHorizontalGrid(
    modifier: Modifier = Modifier,
    libraries: Libs?,
) {
    val libs = remember(libraries) {
        libraries?.libraries.orEmpty().distinctBy(Library::name)
    }
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
            .clip(LibraryShape)
            .clickable {
                if (library.licenses.none { it.htmlReadyLicenseContent.isNullOrBlank() }) {
                    onLibraryClicked(library)
                }
            }
            .padding(horizontal = 8.dp),
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

    SimpleDialog(
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
                PrimaryDialogButton(
                    text = stringResource(Res.string.view_website),
                    onClick = {
                        runCatching { uriHandler.openUri(website) }
                    },
                )
            }
        },
        dismissButton = {
            NeutralDialogButton(
                stringResource(Res.string.close),
                onClick = onDismiss,
            )
        },
    )
}

private val LibraryShape = RoundedCornerShape(8.dp)
