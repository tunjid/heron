/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.inference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.inference.ui.ModelCard
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import heron.feature.inference.generated.resources.Res
import heron.feature.inference.generated.resources.load_default_model_on_launch
import heron.feature.inference.generated.resources.no_default_model
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InferenceScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topClearance = UiTokens.statusBarHeight + UiTokens.toolbarHeight
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topClearance),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                top = 8.dp,
                horizontal = 16.dp,
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(
                key = "load-default-model-toggle",
            ) {
                LoadDefaultModelToggle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    loadOnLaunch = state.loadDefaultModelOnLaunch,
                    defaultModelName = state.defaultModelName,
                    onCheckedChange = {
                        actions(Action.SetLoadDefaultModelOnLaunch(it))
                    },
                )
            }
            items(
                items = state.models,
                key = { it.model.name },
                itemContent = { item ->
                    ModelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        engineState = state.engineState,
                        item = item,
                        onLoad = {
                            actions(Action.Load(it))
                        },
                        onDownload = {
                            actions(Action.Download(item.model))
                        },
                        onCancel = {
                            actions(Action.Cancel(item.model))
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun LoadDefaultModelToggle(
    loadOnLaunch: Boolean,
    defaultModelName: String?,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.load_default_model_on_launch),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = defaultModelName ?: stringResource(Res.string.no_default_model),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = loadOnLaunch,
                enabled = defaultModelName != null,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
