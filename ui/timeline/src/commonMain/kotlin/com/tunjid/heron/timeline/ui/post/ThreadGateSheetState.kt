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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.allowedListUrisOrEmpty
import com.tunjid.heron.data.core.models.allowsAll
import com.tunjid.heron.data.core.models.allowsFollowers
import com.tunjid.heron.data.core.models.allowsFollowing
import com.tunjid.heron.data.core.models.allowsMentioned
import com.tunjid.heron.data.core.models.allowsNone
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.collapse_icon
import heron.ui.core.generated.resources.expand_icon
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.thread_gate_anyone
import heron.ui.timeline.generated.resources.thread_gate_info
import heron.ui.timeline.generated.resources.thread_gate_nobody
import heron.ui.timeline.generated.resources.thread_gate_people_you_follow
import heron.ui.timeline.generated.resources.thread_gate_people_you_mention
import heron.ui.timeline.generated.resources.thread_gate_post_reply_settings
import heron.ui.timeline.generated.resources.thread_gate_save
import heron.ui.timeline.generated.resources.thread_gate_select_from_your_lists
import heron.ui.timeline.generated.resources.thread_gate_who_can_reply
import heron.ui.timeline.generated.resources.thread_gate_your_followers
import org.jetbrains.compose.resources.stringResource

@Stable
sealed class ThreadGateSheetState private constructor(
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    protected var mode by mutableStateOf<Mode?>(null)
    internal var allowed by mutableStateOf<ThreadGate.Allowed?>(null)

    internal val isForSinglePost get() = mode is Mode.Timeline

    internal inline fun updateAllowed(
        block: ThreadGate.Allowed.() -> ThreadGate.Allowed?,
    ) {
        allowed = (allowed ?: NoneAllowed).block()
    }

    internal inline fun onUpdated(
        block: (Mode, ThreadGate.Allowed?) -> Unit,
    ) {
        val currentMode = mode
        val currentAllowed = allowed
        if (currentMode != null) block(currentMode, currentAllowed)
    }

    override fun onHidden() {
        mode = null
        allowed = null
    }

    @Stable
    class OfTimeline(
        scope: BottomSheetScope,
    ) : ThreadGateSheetState(scope) {

        fun show(
            timelineItem: TimelineItem,
        ) {
            this.mode = Mode.Timeline(timelineItem)
            this.allowed = timelineItem.threadGate?.allowed

            show()
        }
    }

    @Stable
    class OfPreference(
        scope: BottomSheetScope,
    ) : ThreadGateSheetState(scope) {
        fun show(
            preference: PostInteractionSettingsPreference?,
        ) {
            this.mode = Mode.Preferences(preference)
            this.allowed = preference?.threadGateAllowed

            show()
        }
    }

    companion object {

        @Composable
        fun rememberUpdatedThreadGateSheetState(
            recentLists: List<FeedList>,
            onRequestRecentLists: () -> Unit,
            onThreadGateUpdated: (Post.Interaction.Upsert.Gate) -> Unit,
        ): OfTimeline = rememberUpdatedGenericThreadGateSheetState(
            recentLists = recentLists,
            onRequestRecentLists = onRequestRecentLists,
            ThreadGateSheetState::OfTimeline,
        ) { mode, allowed ->
            require(mode is Mode.Timeline)
            onThreadGateUpdated(mode.update(allowed))
        }

        @Composable
        fun rememberUpdatedThreadGateSheetState(
            recentLists: List<FeedList>,
            onRequestRecentLists: () -> Unit,
            onDefaultThreadGateUpdated: (PostInteractionSettingsPreference) -> Unit,
        ): OfPreference = rememberUpdatedGenericThreadGateSheetState(
            recentLists = recentLists,
            onRequestRecentLists = onRequestRecentLists,
            ThreadGateSheetState::OfPreference,
        ) { mode, allowed ->
            require(mode is Mode.Preferences)
            onDefaultThreadGateUpdated(mode.update(allowed))
        }

        @Composable
        private inline fun <T : ThreadGateSheetState> rememberUpdatedGenericThreadGateSheetState(
            recentLists: List<FeedList>,
            noinline onRequestRecentLists: () -> Unit,
            crossinline initializer: (BottomSheetScope) -> T,
            crossinline onThreadGateUpdated: (Mode, ThreadGate.Allowed?) -> Unit,
        ): T {
            val state = rememberBottomSheetState {
                initializer(it)
            }

            ThreadGateBottomSheet(
                state = state,
                recentLists = recentLists,
                onRequestRecentLists = onRequestRecentLists,
                onThreadGateUpdated = { mode, allowed ->
                    onThreadGateUpdated(mode, allowed)
                },
            )

            return state
        }
    }
}

@Composable
private fun ThreadGateBottomSheet(
    state: ThreadGateSheetState,
    recentLists: List<FeedList>,
    onRequestRecentLists: () -> Unit,
    onThreadGateUpdated: (Mode, ThreadGate.Allowed?) -> Unit,
) {
    var listsExpanded by remember { mutableStateOf(false) }

    state.ModalBottomSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isForSinglePost) Text(
                text = stringResource(Res.string.thread_gate_post_reply_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            else InfoBanner()

            Text(
                text = stringResource(Res.string.thread_gate_who_can_reply),
                style = MaterialTheme.typography.titleSmall,
            )

            // Anyone / Nobody Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectionCard(
                    text = stringResource(Res.string.thread_gate_anyone),
                    selected = state.allowed.allowsAll,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.updateAllowed { null }
                    },
                )
                SelectionCard(
                    text = stringResource(Res.string.thread_gate_nobody),
                    selected = state.allowed.allowsNone,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        state.updateAllowed { NoneAllowed }
                    },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val isCustomOrAnyone = !state.allowed.allowsNone

                SettingsCheckboxRow(
                    text = stringResource(Res.string.thread_gate_your_followers),
                    checked = state.allowed.allowsFollowers,
                    enabled = isCustomOrAnyone,
                    onClick = {
                        state.updateAllowed { copy(allowsFollowers = !allowsFollowers) }
                    },
                )
                SettingsCheckboxRow(
                    text = stringResource(Res.string.thread_gate_people_you_follow),
                    checked = state.allowed.allowsFollowing,
                    enabled = isCustomOrAnyone,
                    onClick = {
                        state.updateAllowed { copy(allowsFollowing = !allowsFollowing) }
                    },
                )
                SettingsCheckboxRow(
                    text = stringResource(Res.string.thread_gate_people_you_mention),
                    checked = state.allowed.allowsMentioned,
                    enabled = isCustomOrAnyone,
                    onClick = {
                        state.updateAllowed { copy(allowsMentioned = !allowsMentioned) }
                    },
                )
            }

            val selectedUris = state.allowed.allowedListUrisOrEmpty
            val enabled = !state.allowed.allowsNone

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            if (!listsExpanded) onRequestRecentLists()
                            listsExpanded = !listsExpanded
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.thread_gate_select_from_your_lists),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Icon(
                        imageVector = if (listsExpanded)
                            Icons.Rounded.KeyboardArrowUp
                        else
                            Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (listsExpanded) CommonStrings.collapse_icon
                            else CommonStrings.expand_icon,
                        ),
                    )
                }

                AnimatedVisibility(visible = listsExpanded && enabled) {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        recentLists.forEach { list ->
                            val checked = list.uri in selectedUris

                            FeedListCheckboxRow(
                                list = list,
                                checked = checked,
                                enabled = enabled,
                                onClick = {
                                    state.updateAllowed {
                                        val newLists =
                                            if (checked) allowedListUris - list.uri
                                            else allowedListUris + list.uri

                                        copy(allowedListUris = newLists)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Quote Posts Toggle
            // Comment out for now, out of scope
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)) // Darker blue in screenshot
//                    .padding(horizontal = 16.dp, vertical = 12.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(12.dp),
//                ) {
//                    Icon(
//                        imageVector = Icons.Rounded.FormatQuote,
//                        contentDescription = null,
//                        modifier = Modifier.size(20.dp),
//                    )
//                    Text(
//                        text = stringResource(Res.string.thread_gate_allow_quote_posts),
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.Medium,
//                    )
//                }
//                Switch(
//                    checked = true,
//                    onCheckedChange = {
//
//                    },
//                )
//            }

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    state.onUpdated(onThreadGateUpdated)
                    state.hide()
                },
            ) {
                Text(stringResource(Res.string.thread_gate_save))
            }
        }
    }
}

@Composable
private fun InfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
            )
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(Res.string.thread_gate_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun SelectionCard(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    else
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)

    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        Color.Transparent

    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // Handled by parent row
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SettingsCheckboxRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null, // Handled by parent row for larger touch target
            enabled = enabled,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f,
            ),
        )
    }
}

@Composable
private fun FeedListCheckboxRow(
    list: FeedList,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )

        Spacer(Modifier.width(12.dp))

        AsyncImage(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            args = remember(list.avatar) {
                ImageArgs(
                    url = list.avatar?.uri,
                    contentScale = ContentScale.Crop,
                    contentDescription = list.name,
                    shape = RoundedPolygonShape.Circle,
                )
            },
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = list.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

sealed class Mode {

    data class Timeline(
        val item: TimelineItem,
    ) : Mode() {
        internal fun update(allowed: ThreadGate.Allowed?) = Post.Interaction.Upsert.Gate(
            postUri = item.post.uri,
            threadGateUri = item.threadGate?.uri,
            allowsFollowing = allowed.allowsFollowing,
            allowsFollowers = allowed.allowsFollowers,
            allowsMentioned = allowed.allowsMentioned,
            allowedListUris = allowed.allowedListUrisOrEmpty,
        )
    }

    data class Preferences(
        val preference: PostInteractionSettingsPreference?,
    ) : Mode() {
        internal fun update(
            allowed: ThreadGate.Allowed?,
        ) = PostInteractionSettingsPreference(
            threadGateAllowed = allowed,
            allowedEmbeds = preference?.allowedEmbeds,
        )
    }
}

private val NoneAllowed = ThreadGate.Allowed(
    allowsFollowing = false,
    allowsFollowers = false,
    allowsMentioned = false,
    allowedLists = emptyList(),
)
