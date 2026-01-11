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

package com.tunjid.heron.timeline.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.dismiss
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.label_source
import heron.ui.timeline.generated.resources.view_labeler
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppliedLabelDialog(
    languageTag: String,
    label: Label,
    appliedLabels: AppliedLabels,
    onDismiss: () -> Unit,
    onLabelerClicked: (Labeler) -> Unit,
) {
    appliedLabels.withPreferredLabelerAndLocaleInfo(
        languageTag = languageTag,
        label = label,
    ) { labeler, localeInfo ->
        SimpleDialog(
            onDismissRequest = onDismiss,
            title = {
                SimpleDialogTitle(text = localeInfo.name)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SimpleDialogText(
                        text = localeInfo.description,
                    )
                    SimpleDialogText(
                        text = stringResource(
                            Res.string.label_source,
                            labeler.creator.handle.id,
                        ),
                    )
                }
            },
            dismissButton = {
                NeutralDialogButton(
                    text = stringResource(CommonStrings.dismiss),
                    onClick = onDismiss,
                )
            },
            confirmButton = {
                PrimaryDialogButton(
                    text = stringResource(Res.string.view_labeler),
                    onClick = {
                        onLabelerClicked(labeler)
                    },
                )
            },
        )
    }
}

inline fun AppliedLabels.forEach(
    languageTag: String,
    labels: List<Label>,
    block: (Label, Labeler, Labeler.LocaleInfo) -> Unit,
) {
    labels.forEach { label ->
        withPreferredLabelerAndLocaleInfo(
            languageTag = languageTag,
            label = label,
            block = { labeler, localeInfo ->
                block(label, labeler, localeInfo)
            },
        )
    }
}

inline fun AppliedLabels.withPreferredLabelerAndLocaleInfo(
    languageTag: String,
    label: Label,
    block: (Labeler, Labeler.LocaleInfo) -> Unit,
) {
    val visibility = visibility(label.value)
    if (visibility != Label.Visibility.Warn) return

    val definition = definition(label) ?: return
    val labeler = labeler(label) ?: return

    definition.locale(languageTag)?.let { localeInfo ->
        block(labeler, localeInfo)
    }
}
