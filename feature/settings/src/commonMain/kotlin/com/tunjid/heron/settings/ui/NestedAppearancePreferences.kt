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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.scaffold.ui.theme.Theme
import com.tunjid.heron.scaffold.ui.theme.ordinal
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.autohide_bottom_navigation
import heron.feature.settings.generated.resources.theme
import heron.feature.settings.generated.resources.theme_agami
import heron.feature.settings.generated.resources.theme_black
import heron.feature.settings.generated.resources.theme_blue
import heron.feature.settings.generated.resources.theme_capped
import heron.feature.settings.generated.resources.theme_default
import heron.feature.settings.generated.resources.theme_dynamic
import heron.feature.settings.generated.resources.theme_green
import heron.feature.settings.generated.resources.theme_reddish
import heron.feature.settings.generated.resources.theme_tricolored
import heron.feature.settings.generated.resources.use_compact_navigation
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSection(
    signedInProfilePreferences: Preferences,
    setCurrentThemeOrdinal: (Int) -> Unit,
    setCompactNavigation: (Boolean) -> Unit,
    setAutoHideBottomNavigation: (Boolean) -> Unit,
) {
    val isDynamicThemingSupported = isDynamicThemingSupported()
    val isCompactNavigationSupported = isCompactNavigationSupported()

    val availableThemes = remember(isDynamicThemingSupported) {
        Theme.entries.filter { theme ->
            theme != Theme.Dynamic || isDynamicThemingSupported
        }
    }

    SettingsRadioButtons(
        modifier = Modifier
            .fillMaxWidth(),
        title = Res.string.theme,
        selectedItem = Theme.fromOrdinal(
            signedInProfilePreferences.local.currentThemeOrdinal,
        ),
        items = availableThemes,
        itemStringResource = Theme::themeStringResource,
        onItemClicked = { theme -> setCurrentThemeOrdinal(theme.ordinal) },
    )

    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
    )

    SettingsToggleItem(
        modifier = Modifier
            .fillMaxWidth(),
        text = stringResource(Res.string.use_compact_navigation),
        enabled = isCompactNavigationSupported,
        checked = signedInProfilePreferences.local.useCompactNavigation,
        onCheckedChange = setCompactNavigation,
    )
    SettingsToggleItem(
        modifier = Modifier
            .fillMaxWidth(),
        text = stringResource(Res.string.autohide_bottom_navigation),
        enabled = isCompactNavigationSupported,
        checked = signedInProfilePreferences.local.autoHideBottomNavigation,
        onCheckedChange = setAutoHideBottomNavigation,
    )
}

private fun Theme.themeStringResource(): StringResource = when (this) {
    Theme.Default -> Res.string.theme_default
    Theme.Dynamic -> Res.string.theme_dynamic
    Theme.Herons.Agami -> Res.string.theme_agami
    Theme.Herons.Black -> Res.string.theme_black
    Theme.Herons.Blue -> Res.string.theme_blue
    Theme.Herons.Capped -> Res.string.theme_capped
    Theme.Herons.Green -> Res.string.theme_green
    Theme.Herons.Reddish -> Res.string.theme_reddish
    Theme.Herons.Tricolored -> Res.string.theme_tricolored
}
