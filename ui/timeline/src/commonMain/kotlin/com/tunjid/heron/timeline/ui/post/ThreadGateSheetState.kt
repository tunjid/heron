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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.allowsFollowers
import com.tunjid.heron.data.core.models.allowsFollowing
import com.tunjid.heron.data.core.models.allowsMentioned
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState

@Stable
class ThreadGateSheetState private constructor(
    threadGate: ThreadGate?,
    scope: BottomSheetScope,
) : BottomSheetState(scope) {

    // UI State for the settings
    var replyPolicy by mutableStateOf<ReplyPolicy>(ReplyPolicy.Anyone)
    var allowQuotePosts by mutableStateOf(true)

    // Specific boolean flags for the custom policy
    var allowFollowers by mutableStateOf(threadGate.allowsFollowers)
    var allowFollows by mutableStateOf(threadGate.allowsFollowing)
    var allowMentions by mutableStateOf(threadGate.allowsMentioned)

    var threadGate by mutableStateOf(false)

    // Callback for when the user clicks Save
    internal var onSave: ((ReplyPolicy, Boolean) -> Unit)? = null

    fun showSettings(
        currentPolicy: ReplyPolicy = ReplyPolicy.Anyone,
        currentAllowQuotePosts: Boolean = true,
        onSave: (ReplyPolicy, Boolean) -> Unit,
    ) {
        this.replyPolicy = currentPolicy
        this.allowQuotePosts = currentAllowQuotePosts
        this.onSave = onSave

        // Initialize checkboxes based on policy if it's custom
        if (currentPolicy is ReplyPolicy.Specific) {
            allowFollowers = currentPolicy.followers
            allowFollows = currentPolicy.follows
            allowMentions = currentPolicy.mentions
        } else {
            allowFollowers = false
            allowFollows = false
            allowMentions = false
        }

        show()
    }

    override fun onHidden() {
        onSave = null
    }

    fun setPolicy(newPolicy: ReplyPolicy) {
        replyPolicy = newPolicy
        // Reset specific toggles if moving to a preset
        if (newPolicy is ReplyPolicy.Anyone || newPolicy is ReplyPolicy.Nobody) {
            allowFollowers = false
            allowFollows = false
            allowMentions = false
        }
    }

    fun toggleSpecific(type: SpecificType) {
        // If we were on Anyone/Nobody, switch to Specific
        if (replyPolicy !is ReplyPolicy.Specific) {
            replyPolicy = ReplyPolicy.Specific()
        }

        when (type) {
            SpecificType.Followers -> allowFollowers = !allowFollowers
            SpecificType.Follows -> allowFollows = !allowFollows
            SpecificType.Mentions -> allowMentions = !allowMentions
        }

        // Update the main policy object to reflect changes
        replyPolicy = ReplyPolicy.Specific(
            followers = allowFollowers,
            follows = allowFollows,
            mentions = allowMentions,
        )
    }

    companion object Companion {
        @Composable
        fun rememberPostRepliesSheetState(
            threadGate: ThreadGate?,
        ): ThreadGateSheetState {
            val state = rememberBottomSheetState {
                ThreadGateSheetState(
                    threadGate = threadGate,
                    scope = it,
                )
            }

            PostRepliesBottomSheet(state = state)

            return state
        }
    }

    enum class SpecificType { Followers, Follows, Mentions }
}

sealed interface ReplyPolicy {
    data object Anyone : ReplyPolicy
    data object Nobody : ReplyPolicy
    data class Specific(
        val followers: Boolean = false,
        val follows: Boolean = false,
        val mentions: Boolean = false,
        // Lists logic omitted for brevity, consistent with screenshot scaffold
    ) : ReplyPolicy
}

@Composable
private fun PostRepliesBottomSheet(
    state: ThreadGateSheetState,
) {
    state.ModalBottomSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Info Box
            InfoBanner()

            Text(
                text = "Who can reply",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // Anyone / Nobody Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectionCard(
                    text = "Anyone",
                    selected = state.replyPolicy is ReplyPolicy.Anyone,
                    modifier = Modifier.weight(1f),
                    onClick = { state.setPolicy(ReplyPolicy.Anyone) },
                )
                SelectionCard(
                    text = "Nobody",
                    selected = state.replyPolicy is ReplyPolicy.Nobody,
                    modifier = Modifier.weight(1f),
                    onClick = { state.setPolicy(ReplyPolicy.Nobody) },
                )
            }

            // Specific Group Checkboxes
            // These are typically disabled if "Nobody" is selected,
            // but functionally accessible if we treat clicking them as switching to "Specific" mode.
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val isCustomOrAnyone = state.replyPolicy !is ReplyPolicy.Nobody

                SettingsCheckboxRow(
                    text = "Your followers",
                    checked = state.allowFollowers,
                    enabled = isCustomOrAnyone,
                    onClick = { state.toggleSpecific(ThreadGateSheetState.SpecificType.Followers) },
                )
                SettingsCheckboxRow(
                    text = "People you follow",
                    checked = state.allowFollows,
                    enabled = isCustomOrAnyone,
                    onClick = { state.toggleSpecific(ThreadGateSheetState.SpecificType.Follows) },
                )
                SettingsCheckboxRow(
                    text = "People you mention",
                    checked = state.allowMentions,
                    enabled = isCustomOrAnyone,
                    onClick = { state.toggleSpecific(ThreadGateSheetState.SpecificType.Mentions) },
                )
            }

            // Lists Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                    .clickable { /* Open Lists selection */ }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Select from your lists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Quote Posts Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)) // Darker blue in screenshot
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Allow quote posts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Switch(
                    checked = state.allowQuotePosts,
                    onCheckedChange = { state.allowQuotePosts = it },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = {
                    state.onSave?.invoke(state.replyPolicy, state.allowQuotePosts)
                    state.hide()
                },
            ) {
                Text("Save")
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
            text = "The following settings will be used as your defaults when creating new posts. You can edit these for a specific post from the composer.",
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
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}
