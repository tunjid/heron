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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.scaffold.navigation.NavigationAction.ReferringRouteOption
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.label.LabelSetting
import com.tunjid.heron.timeline.ui.label.Labeler
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
import heron.ui.core.generated.resources.unknown_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ModerationScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth(),
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
            horizontal = 16.dp,
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
    ) {
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
