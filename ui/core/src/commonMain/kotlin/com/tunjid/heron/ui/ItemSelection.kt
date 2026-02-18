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

package com.tunjid.heron.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
inline fun <T> ItemSelection(
    modifier: Modifier = Modifier,
    alwaysExpanded: Boolean = false,
    selectedItem: T,
    availableItems: List<T>,
    crossinline key: T.() -> String,
    crossinline icon: T.() -> ImageVector,
    crossinline stringResource: T.() -> StringResource,
    crossinline onItemSelected: (T) -> Unit,
) {
    var expandedItem by remember { mutableStateOf(if (alwaysExpanded) selectedItem else null) }
    LookaheadScope {
        ElevatedCard(modifier = modifier, shape = CircleShape) {
            Row(
                modifier = Modifier.animateContentSize(),
                horizontalArrangement = Arrangement.aligned(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (availableItems.size > 1)
                    availableItems.forEach { item ->
                        androidx.compose.runtime.key(item.key()) {
                            val isSelected = selectedItem == item
                            AnimatedVisibility(
                                modifier =
                                    Modifier.animateBounds(lookaheadScope = this@LookaheadScope),
                                visible = isSelected || expandedItem != null,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut(),
                            ) {
                                var entered by remember { mutableStateOf(false) }
                                val progress = animateFloatAsState(if (entered) 1f else 0f)
                                IconButton(
                                    modifier =
                                        Modifier.graphicsLayer { alpha = progress.value }
                                            .size(40.dp),
                                    onClick = {
                                        when (expandedItem) {
                                            null -> expandedItem = item
                                            item -> if (!alwaysExpanded) expandedItem = null
                                            else -> onItemSelected(item)
                                        }
                                    },
                                    content = {
                                        Icon(
                                            modifier = Modifier.size(24.dp),
                                            imageVector = item.icon(),
                                            contentDescription =
                                                org.jetbrains.compose.resources.stringResource(
                                                    item.stringResource()
                                                ),
                                            tint =
                                                when (item) {
                                                    selectedItem ->
                                                        MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                        )
                                    },
                                )
                                LaunchedEffect(Unit) { entered = true }
                            }
                        }
                    }
            }
        }
    }

    DisposableEffect(expandedItem, selectedItem, alwaysExpanded) {
        if (!alwaysExpanded) {
            if (expandedItem != null && expandedItem != selectedItem) {
                expandedItem = null
            }
        }
        onDispose {}
    }
}
