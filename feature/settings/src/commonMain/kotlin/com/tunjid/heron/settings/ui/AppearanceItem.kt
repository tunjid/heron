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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Preferences
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.appearance
import heron.feature.settings.generated.resources.autohide_bottom_navigation
import heron.feature.settings.generated.resources.use_compact_navigation
import heron.feature.settings.generated.resources.use_dynamic_theming
import org.jetbrains.compose.resources.stringResource

@Stable expect fun isDynamicThemingSupported(): Boolean

@Stable expect fun isCompactNavigationSupported(): Boolean

@Composable
fun AppearanceItem(
    modifier: Modifier = Modifier,
    signedInProfilePreferences: Preferences,
    setDynamicThemingPreference: (Boolean) -> Unit,
    setCompactNavigation: (Boolean) -> Unit,
    setAutoHideBottomNavigation: (Boolean) -> Unit,
) {
    val isDynamicThemingSupported = isDynamicThemingSupported()
    val isCompactNavigationSupported = isCompactNavigationSupported()

    ExpandableSettingsItemRow(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(Res.string.appearance),
        icon = Icons.Rounded.Palette,
    ) {
        SettingsToggleItem(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.use_dynamic_theming),
            enabled = isDynamicThemingSupported,
            checked = signedInProfilePreferences.local.useDynamicTheming,
            onCheckedChange = setDynamicThemingPreference,
        )
        SettingsToggleItem(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.use_compact_navigation),
            enabled = isCompactNavigationSupported,
            checked = signedInProfilePreferences.local.useCompactNavigation,
            onCheckedChange = setCompactNavigation,
        )
        SettingsToggleItem(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.autohide_bottom_navigation),
            enabled = isCompactNavigationSupported,
            checked = signedInProfilePreferences.local.autoHideBottomNavigation,
            onCheckedChange = setAutoHideBottomNavigation,
        )
    }
}
