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

package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.profile.LabelerSettingsStateHolder
import com.tunjid.heron.timeline.ui.label.LabelSetting
import com.tunjid.heron.timeline.ui.label.locale
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.profile.generated.resources.Res
import heron.feature.profile.generated.resources.label_hide
import heron.feature.profile.generated.resources.label_off
import heron.feature.profile.generated.resources.label_show_badge
import heron.ui.core.generated.resources.unknown_label

@Composable
fun LabelerSettings(
    modifier: Modifier = Modifier,
    stateHolder: LabelerSettingsStateHolder,
    prefersCompactBottomNav: Boolean,
) {
    val state by stateHolder.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
            isCompact = prefersCompactBottomNav,
        ),
    ) {
        val languageTag = Locale.current.toLanguageTag()
        items(
            items = state.labelSettings,
            itemContent = { labelSetting ->
                Column {
                    val locale = labelSetting.definition.locale(languageTag)
                    LabelSetting(
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        enabled = state.subscribed,
                        labelName = locale?.name ?: labelSetting.definition.identifier.value,
                        labelDescription = locale?.description,
                        selectedVisibility = labelSetting.visibility,
                        visibilities = Label.Visibility.all,
                        visibilityStringResource = Label.Visibility::stringRes,
                        onVisibilityChanged = {
                            stateHolder.accept(labelSetting.copy(visibility = it))
                        },
                    )
                    HorizontalDivider()
                }
            },
        )
    }
}

private val Label.Visibility.stringRes
    get() = when (this) {
        Label.Visibility.Ignore -> Res.string.label_off
        Label.Visibility.Warn -> Res.string.label_show_badge
        Label.Visibility.Hide -> Res.string.label_hide
        else -> CommonStrings.unknown_label
    }
