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
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.notification_permissions_denied_dialog_text
import heron.scaffold.generated.resources.notification_permissions_dialog_title
import heron.scaffold.generated.resources.notification_permissions_request_dialog_text
import heron.ui.core.generated.resources.go_to_settings
import heron.ui.core.generated.resources.no
import heron.ui.core.generated.resources.yes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationsRationaleDialog(
    callingRationale: NotificationDialogRationale,
    shouldRequestPermissions: (NotificationDialogRationale, Boolean) -> Unit,
) {
    SimpleDialog(
        onDismissRequest = {
            shouldRequestPermissions(callingRationale, false)
        },
        title = {
            SimpleDialogTitle(
                text = stringResource(Res.string.notification_permissions_dialog_title),
            )
        },
        text = {
            SimpleDialogText(
                text = stringResource(
                    when (callingRationale) {
                        NotificationDialogRationale.GoToSettings -> Res.string.notification_permissions_denied_dialog_text
                        NotificationDialogRationale.RequestPermissions -> Res.string.notification_permissions_request_dialog_text
                    },
                ),
            )
        },
        confirmButton = {
            PrimaryDialogButton(
                text = stringResource(
                    when (callingRationale) {
                        NotificationDialogRationale.GoToSettings -> CommonStrings.go_to_settings
                        NotificationDialogRationale.RequestPermissions -> CommonStrings.yes
                    },
                ),
                onClick = {
                    shouldRequestPermissions(callingRationale, true)
                },
            )
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(CommonStrings.no),
                onClick = {
                    shouldRequestPermissions(callingRationale, false)
                },
            )
        },
    )
}

internal sealed class NotificationDialogRationale {
    data object RequestPermissions : NotificationDialogRationale()
    data object GoToSettings : NotificationDialogRationale()
}
