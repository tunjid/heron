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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.give_feedback
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedbackItem(
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    SettingsItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                runCatching { uriHandler.openUri(FeedbackLink) }
            }
            .settingsItemPaddingAndMinHeight(),
        title = stringResource(Res.string.give_feedback),
        icon = Icons.Rounded.Feedback,
    )
}

private const val FeedbackLink = "https://github.com/tunjid/heron/issues"
