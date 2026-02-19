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

package com.tunjid.heron.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ConnectWithoutContact
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.scaffold.navigation.blocksDestination
import com.tunjid.heron.scaffold.navigation.mutesDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.label.LabelSetting
import com.tunjid.heron.timeline.ui.label.Labeler
import com.tunjid.heron.timeline.ui.post.ThreadGateSheetState.Companion.rememberUpdatedThreadGateSheetState
import com.tunjid.heron.timeline.ui.sheets.MutedWordsSheetState.Companion.rememberUpdatedMutedWordsSheetState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.moderation.generated.resources.Res
import heron.feature.moderation.generated.resources.content_filters
import heron.feature.moderation.generated.resources.enable_adult_content
import heron.feature.moderation.generated.resources.label_hide
import heron.feature.moderation.generated.resources.label_show
import heron.feature.moderation.generated.resources.label_warn
import heron.feature.moderation.generated.resources.labeler_subscriptions
import heron.feature.moderation.generated.resources.moderation_options_blocked_accounts
import heron.feature.moderation.generated.resources.moderation_options_interaction_settings
import heron.feature.moderation.generated.resources.moderation_options_muted_accounts
import heron.feature.moderation.generated.resources.moderation_options_title
import heron.feature.moderation.generated.resources.mute_words_tags
import heron.ui.core.generated.resources.unknown_label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModerationScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val threadGateSheetState = rememberUpdatedThreadGateSheetState(
        recentLists = emptyList(),
        onRequestRecentLists = { },
        onDefaultThreadGateUpdated = {
            actions(Action.UpdateThreadGates(it))
        },
    )
    val mutedWordSheetState = rememberUpdatedMutedWordsSheetState(
        mutedWordPreferences = state.preferences.mutedWordPreferences,
        onSave = { actions(Action.UpdateMutedWord(it)) },
        onShown = { },
    )
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
            horizontal = 16.dp,
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
    ) {
        moderationToolsMenuSection(
            onInteractionSettingsClicked = {
                threadGateSheetState.show(
                    preference = state.preferences.postInteractionSettings,
                )
            },
            onMutedWordsClicked = mutedWordSheetState::show,
            navigate = {
                actions(Action.Navigate.To(it))
            },
        )
        adultLabelsSection(
            adultContentEnabled = state.adultContentEnabled,
            adultLabelItems = state.adultLabelItems,
            onAdultLabelVisibilityChanged = { adultLabel, visibility ->
                actions(
                    Action.UpdateAdultLabelVisibility(
                        adultLabel = adultLabel.adult,
                        visibility = visibility,
                    ),
                )
            },
            onAdultPreferencesChecked = { adultContentEnabled ->
                actions(
                    Action.UpdateAdultContentPreferences(
                        adultContentEnabled = adultContentEnabled,
                    ),
                )
            },
        )
        subscribedLabelersSection(
            paneScaffoldState = paneScaffoldState,
            labelers = state.subscribedLabelers,
            onLabelerClicked = { labeler ->
                actions(
                    Action.Navigate.To(
                        profileDestination(
                            profile = labeler.creator,
                            avatarSharedElementKey = labeler.avatarSharedElementKey(Moderation),
                            referringRouteOption = ReferringRouteOption.Current,
                        ),
                    ),
                )
            },
        )
    }
}

private fun LazyListScope.adultLabelsSection(
    adultContentEnabled: Boolean,
    adultLabelItems: List<AdultLabelItem>,
    onAdultLabelVisibilityChanged: (AdultLabelItem, Label.Visibility) -> Unit,
    onAdultPreferencesChecked: (Boolean) -> Unit,
) {
    item(
        key = Res.string.content_filters.key,
    ) {
        SectionTitle(
            modifier = Modifier
                .animateItem(),
            title = stringResource(Res.string.content_filters),
        )
    }
    item(
        key = Res.string.enable_adult_content.key,
    ) {
        ElevatedItem(
            modifier = Modifier
                .animateItem(),
            shape = if (adultContentEnabled) FirstCardShape else RoundCardShape,
            showDivider = adultContentEnabled,
        ) {
            Row(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.enable_adult_content))
                Switch(
                    checked = adultContentEnabled,
                    onCheckedChange = onAdultPreferencesChecked,
                )
            }
        }
    }
    if (adultContentEnabled) itemsIndexed(
        items = adultLabelItems,
        key = { _, label ->
            label.nameRes.key
        },
        itemContent = { index, item ->
            val isLastLabel = index == adultLabelItems.lastIndex
            ElevatedItem(
                modifier = Modifier
                    .animateItem(),
                shape = if (isLastLabel) LastCardShape else RectangleShape,
                showDivider = !isLastLabel,
            ) {
                LabelSetting(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    enabled = true,
                    labelName = stringResource(item.nameRes),
                    labelDescription = stringResource(item.descriptionRes),
                    selectedVisibility = item.visibility,
                    visibilities = Label.Visibility.all,
                    visibilityStringResource = Label.Visibility::stringRes,
                    onVisibilityChanged = { visibility ->
                        onAdultLabelVisibilityChanged(item, visibility)
                    },
                )
            }
        },
    )
}

private fun LazyListScope.subscribedLabelersSection(
    paneScaffoldState: PaneScaffoldState,
    labelers: List<Labeler>,
    onLabelerClicked: (Labeler) -> Unit,
) {
    item(
        key = Res.string.labeler_subscriptions.key,
    ) {
        SectionTitle(
            modifier = Modifier
                .animateItem(),
            title = stringResource(Res.string.labeler_subscriptions),
        )
    }

    itemsIndexed(
        items = labelers,
        key = { _, labeler ->
            labeler.uri.uri
        },
        itemContent = { index, labeler ->
            val isLastLabel = index == labelers.lastIndex
            ElevatedItem(
                modifier = Modifier
                    .animateItem(),
                shape = when {
                    index == 0 && isLastLabel -> RoundCardShape
                    index == 0 -> FirstCardShape
                    isLastLabel -> LastCardShape
                    else -> RectangleShape
                },
                showDivider = !isLastLabel,
                onItemClicked = {
                    onLabelerClicked(labeler)
                },
            ) {
                Labeler(
                    modifier = Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    movableElementSharedTransitionScope = paneScaffoldState,
                    sharedElementPrefix = Moderation,
                    labeler = labeler,
                )
            }
        },
    )
}

private fun LazyListScope.moderationToolsMenuSection(
    onInteractionSettingsClicked: () -> Unit,
    onMutedWordsClicked: () -> Unit,
    navigate: (NavigationAction.Destination) -> Unit,
) {
    item(
        key = Res.string.moderation_options_title.key,
    ) {
        SectionTitle(
            modifier = Modifier.animateItem(),
            title = stringResource(Res.string.moderation_options_title),
        )
    }
    itemsIndexed(
        items = ModerationTools.entries,
        key = { _, tool ->
            tool.stringResource.key
        },
        itemContent = { index, tool ->
            val isFirstItem = index == 0
            val isLastItem = index == ModerationTools.entries.lastIndex
            ElevatedItem(
                modifier = Modifier
                    .animateItem(),
                shape =
                if (isFirstItem) FirstCardShape
                else if (isLastItem) LastCardShape
                else RectangleShape,
                showDivider = !isLastItem,
                onItemClicked = {
                    when (tool) {
                        ModerationTools.InteractionSettings -> onInteractionSettingsClicked()
                        ModerationTools.MutedWords -> onMutedWordsClicked()
                        ModerationTools.BlockedAccounts -> navigate(blocksDestination())
                        ModerationTools.MutedAccounts -> navigate(mutesDestination())
                    }
                },
            ) {
                ModerationItemRow(
                    title = stringResource(tool.stringResource),
                    icon = tool.icon,
                )
            }
        },
    )
}

@Composable
private fun ElevatedItem(
    modifier: Modifier = Modifier,
    shape: Shape,
    showDivider: Boolean,
    onItemClicked: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onItemClicked == null) ElevatedCard(
        modifier = modifier,
        shape = shape,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
            if (showDivider) HorizontalDivider()
        }
    }
    else ElevatedCard(
        modifier = modifier,
        shape = shape,
        onClick = onItemClicked,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
            if (showDivider) HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier
            .padding(vertical = 16.dp),
        text = title,
        style = MaterialTheme.typography.titleMediumEmphasized,
    )
}

@Composable
private fun ModerationItemRow(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private enum class ModerationTools(
    val stringResource: StringResource,
    val icon: ImageVector,
) {
    InteractionSettings(
        stringResource = Res.string.moderation_options_interaction_settings,
        icon = Icons.Rounded.ConnectWithoutContact,
    ),
    MutedWords(
        stringResource = Res.string.mute_words_tags,
        icon = Icons.Rounded.FilterAlt,
    ),
    MutedAccounts(
        stringResource = Res.string.moderation_options_muted_accounts,
        icon = Icons.AutoMirrored.Rounded.VolumeOff,
    ),
    BlockedAccounts(
        stringResource = Res.string.moderation_options_blocked_accounts,
        icon = Icons.Rounded.Block,
    ),
}

private val FirstCardShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
)

private val LastCardShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)

private val Label.Visibility.stringRes
    get() = when (this) {
        Label.Visibility.Ignore -> Res.string.label_show
        Label.Visibility.Warn -> Res.string.label_warn
        Label.Visibility.Hide -> Res.string.label_hide
        else -> CommonStrings.unknown_label
    }

private val RoundCardShape = RoundedCornerShape(16.dp)

private const val Moderation = "moderation"
