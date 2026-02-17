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

package com.tunjid.heron.graze.editor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tunjid.heron.data.graze.GrazeFeed
import com.tunjid.heron.data.graze.isValid
import com.tunjid.heron.ui.AppBarButton
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.graze_editor.generated.resources.Res
import heron.feature.graze_editor.generated.resources.delete_feed
import heron.feature.graze_editor.generated.resources.edit_feed
import heron.ui.core.generated.resources.more_options
import heron.ui.core.generated.resources.save
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopBarActions(
    grazeFeed: GrazeFeed.Editable,
    enabled: Boolean,
    onEditClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
) {
    AppBarButton(
        icon = Icons.Rounded.Save,
        enabled = enabled && grazeFeed.filter.isValid,
        iconDescription = stringResource(CommonStrings.save),
        onClick = onSaveClicked,
    )
    Box {
        var showMenu by remember { mutableStateOf(false) }
        IconButton(
            enabled = enabled,
            onClick = {
                showMenu = true
            },
            content = {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(CommonStrings.more_options),
                )
            },
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.edit_feed)) },
                onClick = {
                    showMenu = false
                    onEditClicked()
                },
            )
            if (grazeFeed is GrazeFeed.Created) DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete_feed)) },
                onClick = {
                    showMenu = false
                    onDeleteClicked()
                },
            )
        }
    }
}
