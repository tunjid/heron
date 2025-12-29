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

package com.tunjid.heron.scaffold.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.notification_permissions_dialog_text
import heron.scaffold.generated.resources.notification_permissions_dialog_title
import heron.ui.core.generated.resources.no
import heron.ui.core.generated.resources.yes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationsRationaleDialog(
    shouldRequestPermissions: (Boolean) -> Unit,
) {
    var dismissed by rememberSaveable { mutableStateOf(false) }

    if (!dismissed) {
        SimpleDialog(
            onDismissRequest = {
                shouldRequestPermissions(false)
                dismissed = true
            },
            title = {
                SimpleDialogTitle(
                    text = stringResource(Res.string.notification_permissions_dialog_title),
                )
            },
            text = {
                SimpleDialogText(
                    text = stringResource(Res.string.notification_permissions_dialog_text),
                )
            },
            confirmButton = {
                PrimaryDialogButton(
                    text = stringResource(CommonStrings.yes),
                    onClick = {
                        shouldRequestPermissions(true)
                        dismissed = true
                    },
                )
            },
            dismissButton = {
                NeutralDialogButton(
                    text = stringResource(CommonStrings.no),
                    onClick = {
                        shouldRequestPermissions(false)
                        dismissed = true
                    },
                )
            },
        )
    }
}
