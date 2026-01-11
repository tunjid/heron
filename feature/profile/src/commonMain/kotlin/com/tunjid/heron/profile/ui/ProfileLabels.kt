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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Labelers
import com.tunjid.heron.data.core.models.ProfileViewerState
import com.tunjid.heron.data.core.models.isBlocked
import com.tunjid.heron.data.core.models.isMuted
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.AppliedLabelDialog
import com.tunjid.heron.timeline.utilities.Label
import com.tunjid.heron.timeline.utilities.LabelFlowRow
import com.tunjid.heron.timeline.utilities.LabelText
import com.tunjid.heron.timeline.utilities.forEach
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.post_author_label
import heron.ui.core.generated.resources.viewer_state_blocked
import heron.ui.core.generated.resources.viewer_state_muted
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileLabels(
    adultContentEnabled: Boolean,
    viewerState: ProfileViewerState?,
    labels: List<Label>,
    labelers: Labelers,
    contentLabelPreferences: ContentLabelPreferences,
    onLabelerClicked: (Labeler) -> Unit,
) {
    val languageTag = Locale.current.toLanguageTag()
    var selectedLabel by remember {
        mutableStateOf<Label?>(null)
    }
    val appliedLabels = remember(
        adultContentEnabled,
        labels,
        labelers,
        contentLabelPreferences,
    ) {
        AppliedLabels(
            adultContentEnabled = adultContentEnabled,
            labels = labels,
            labelers = labelers,
            contentLabelPreferences = contentLabelPreferences,
        )
    }
    LabelFlowRow {
        if (viewerState.isBlocked) IconLabel(
            icon = Icons.Rounded.Block,
            contentDescription = stringResource(CommonStrings.viewer_state_blocked),
            onClick = {},
        )
        if (viewerState.isMuted) IconLabel(
            icon = Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = stringResource(CommonStrings.viewer_state_muted),
            onClick = { },
        )

        appliedLabels.forEach(
            languageTag = languageTag,
            labels = labels,
        ) { label, labeler, localeInfo ->
            val authorLabelContentDescription = stringResource(
                CommonStrings.post_author_label,
                localeInfo.description,
            )
            Label(
                isElevated = true,
                modifier = Modifier
                    .padding(2.dp),
                contentDescription = authorLabelContentDescription,
                icon = {
                    AsyncImage(
                        args = remember(labeler.creator.avatar) {
                            ImageArgs(
                                url = labeler.creator.avatar?.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                shape = RoundedPolygonShape.Circle,
                            )
                        },
                        modifier = Modifier
                            .size(ProfileLabelIconSize),
                    )
                },
                description = {
                    LabelText(localeInfo.name)
                },
                onClick = {
                    selectedLabel = label
                },
            )
        }
        selectedLabel?.let { label ->
            AppliedLabelDialog(
                label = label,
                languageTag = languageTag,
                appliedLabels = appliedLabels,
                onDismiss = {
                    selectedLabel = null
                },
                onLabelerClicked = { labeler ->
                    selectedLabel = null
                    onLabelerClicked(labeler)
                },
            )
        }
    }
}

@Composable
private fun IconLabel(
    icon: ImageVector?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Label(
        isElevated = true,
        contentDescription = contentDescription,
        icon = {
            if (icon != null) Icon(
                modifier = Modifier
                    .size(ProfileLabelIconSize),
                imageVector = icon,
                contentDescription = null,
            )
        },
        description = {
            LabelText(contentDescription)
        },
        onClick = onClick,
    )
}

private val ProfileLabelIconSize = 20.dp
