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

package com.tunjid.heron.editprofile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.ProfileTab
import com.tunjid.heron.data.core.types.FeedGeneratorUri
import com.tunjid.heron.editprofile.ui.TabEditorState.Companion.rememberTabEditorState
import com.tunjid.heron.editprofile.ui.TabEditorState.Companion.tabEditorDragAndDrop
import com.tunjid.heron.editprofile.ui.TabEditorState.Companion.tabEditorDropTarget
import com.tunjid.heron.profile.stringResource
import com.tunjid.heron.ui.JiggleBox
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.fillMaxRestrictedWidth
import com.tunjid.heron.ui.modifiers.chipBackground
import com.tunjid.heron.ui.modifiers.roundedBorder
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.other_tabs
import heron.feature.edit_profile.generated.resources.shown_tabs
import heron.feature.edit_profile.generated.resources.tab_drop_target_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun TabEditor(
    modifier: Modifier = Modifier,
    editableTabs: List<ProfileTab>,
    currentTabs: Set<ProfileTab>,
    feedUrisToFeeds: Map<FeedGeneratorUri, FeedGenerator>,
    onPinnedTabsChanged: (List<ProfileTab>) -> Unit,
) {
    LookaheadScope {
        val tabEditorState = rememberTabEditorState(
            tabs = editableTabs,
            currentTabs = currentTabs,
        )
        Box(
            modifier = modifier
                .fillMaxSize(),
        ) {
            FlowRow(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .fillMaxRestrictedWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val (pinned, saved) = tabEditorState.partitioned

                key(Res.string.shown_tabs) {
                    SectionTitle(
                        modifier = Modifier
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                            ),
                        title = stringResource(Res.string.shown_tabs),
                    )
                }
                pinned.forEach { tab ->
                    key(tab) {
                        if (!tabEditorState.isDragged(tab)) TabChip(
                            modifier = Modifier
                                .animateBounds(lookaheadScope = this@LookaheadScope),
                            tabEditorState = tabEditorState,
                            tab = tab,
                            feedUrisToFeeds = feedUrisToFeeds,
                        )
                    }
                }
                key(Res.string.other_tabs) {
                    SectionTitle(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .animateBounds(lookaheadScope = this@LookaheadScope),
                        title = stringResource(Res.string.other_tabs),
                    )
                }
                saved.forEach { tab ->
                    key(tab) {
                        if (!tabEditorState.isDragged(tab)) TabChip(
                            modifier = Modifier
                                .animateBounds(lookaheadScope = this@LookaheadScope),
                            tabEditorState = tabEditorState,
                            tab = tab,
                            feedUrisToFeeds = feedUrisToFeeds,
                        )
                    }
                }
                AnimatedVisibility(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    visible = tabEditorState.shouldShowHint,
                ) {
                    DropTargetBox(
                        modifier = Modifier
                            .tabEditorDropTarget(
                                state = tabEditorState,
                            )
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        isHovered = tabEditorState.isHintHovered,
                    )
                }

                LaunchedEffect(pinned) {
                    onPinnedTabsChanged(pinned)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SectionTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier
            .padding(
                vertical = 8.dp,
            )
            .fillMaxWidth(),
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
    )
}

@Composable
private fun TabChip(
    modifier: Modifier = Modifier,
    tabEditorState: TabEditorState,
    tab: ProfileTab,
    feedUrisToFeeds: Map<FeedGeneratorUri, FeedGenerator>,
) {
    JiggleBox(
        modifier = modifier,
    ) {
        InputChip(
            modifier = Modifier
                .chipBackground(
                    animateColorAsState(
                        if (tabEditorState.isHoveredId(tab)) TabsState.TabBackgroundColor
                        else Color.Transparent,
                    )::value,
                )
                .tabEditorDragAndDrop(
                    state = tabEditorState,
                    tab = tab,
                ),
            shape = CircleShape,
            selected = false,
            onClick = {
            },
            label = {
                Text(
                    modifier = Modifier
                        .width(IntrinsicSize.Max),
                    text = when (tab) {
                        is ProfileTab.Bluesky.FeedGenerators.FeedGenerator -> feedUrisToFeeds[tab.uri]?.displayName
                            ?: stringResource(tab.stringResource)
                        else -> stringResource(tab.stringResource)
                    },
                    maxLines = 1,
                )
            },
        )
    }
}

@Composable
private fun DropTargetBox(
    modifier: Modifier = Modifier,
    isHovered: Boolean,
) {
    Box(
        modifier = modifier
            .roundedBorder(
                isStroked = true,
                cornerRadius = 8::dp,
                borderColor = animateColorAsState(
                    if (isHovered) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                )::value,
                strokeWidth = animateDpAsState(
                    if (isHovered) 4.dp
                    else Dp.Hairline,
                )::value,
            ),
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp),
            text = stringResource(Res.string.tab_drop_target_hint),
        )
    }
}
