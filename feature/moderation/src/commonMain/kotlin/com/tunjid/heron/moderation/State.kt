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

import com.tunjid.heron.data.core.models.ContentLabelPreference
import com.tunjid.heron.data.core.models.ContentLabelPreferences
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.MutedWordPreference
import com.tunjid.heron.data.core.models.PostInteractionSettingsPreference
import com.tunjid.heron.data.core.models.Preferences
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.heron.ui.text.Memo
import heron.ui.core.generated.resources.graphic_media_label
import heron.ui.core.generated.resources.graphic_media_label_description
import heron.ui.core.generated.resources.nudity_label
import heron.ui.core.generated.resources.nudity_label_description
import heron.ui.core.generated.resources.porn_label
import heron.ui.core.generated.resources.porn_label_description
import heron.ui.core.generated.resources.sexual_label
import heron.ui.core.generated.resources.sexual_label_description
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
data class State(
    val adultContentEnabled: Boolean = false,
    @Transient
    val recentLists: List<FeedList> = emptyList(),
    @Transient
    val preferences: Preferences = Preferences.EmptyPreferences,
    @Transient
    val adultLabelItems: List<AdultLabelItem> = emptyList(),
    @Transient
    val subscribedLabelers: List<Labeler> = emptyList(),
    @Transient
    val messages: List<Memo> = emptyList(),
)

data class AdultLabelItem(
    val adult: Label.Adult,
    val visibility: Label.Visibility,
    val nameRes: StringResource,
    val descriptionRes: StringResource,
)

fun adultLabels(
    contentLabelPreferences: ContentLabelPreferences,
): List<AdultLabelItem> {
    val visibilityMap = contentLabelPreferences.associateBy(
        keySelector = ContentLabelPreference::label,
        valueTransform = ContentLabelPreference::visibility,
    )
    return Label.Adult.entries.map { adultLabel ->
        val visibility = adultLabel.labelValues
            .firstNotNullOfOrNull(visibilityMap::get)
            ?: adultLabel.defaultVisibility

        when (adultLabel) {
            Label.Adult.AdultContent -> AdultLabelItem(
                adult = adultLabel,
                visibility = visibility,
                nameRes = CommonStrings.porn_label,
                descriptionRes = CommonStrings.porn_label_description,
            )
            Label.Adult.SexuallySuggestive -> AdultLabelItem(
                adult = adultLabel,
                visibility = visibility,
                nameRes = CommonStrings.sexual_label,
                descriptionRes = CommonStrings.sexual_label_description,
            )
            Label.Adult.GraphicMedia -> AdultLabelItem(
                adult = adultLabel,
                visibility = visibility,
                nameRes = CommonStrings.graphic_media_label,
                descriptionRes = CommonStrings.graphic_media_label_description,
            )
            Label.Adult.NonSexualNudity -> AdultLabelItem(
                adult = adultLabel,
                visibility = visibility,
                nameRes = CommonStrings.nudity_label,
                descriptionRes = CommonStrings.nudity_label_description,
            )
        }
    }
}

sealed class Action(val key: String) {

    data class UpdateAdultLabelVisibility(
        val adultLabel: Label.Adult,
        val visibility: Label.Visibility,
    ) : Action(key = "UpdateAdultLabelVisibility")

    data class UpdateAdultContentPreferences(
        val adultContentEnabled: Boolean,
    ) : Action(key = "UpdateAdultContentPreferences")

    data class UpdateMutedWord(
        val mutedWordPreference: List<MutedWordPreference>,
    ) : Action(key = "UpdateMutedWord")

    data class UpdateThreadGates(
        val preference: PostInteractionSettingsPreference,
    ) : Action(key = "UpdateThreadGates")

    data class SnackbarDismissed(
        val message: Memo,
    ) : Action(key = "SnackbarDismissed")

    data object SignOut : Action(key = "SignOut")

    data object UpdateRecentLists : Action(key = "UpdateRecentLists")

    sealed class Navigate :
        Action(key = "Navigate"),
        NavigationAction {
        data object Pop : Navigate(), NavigationAction by NavigationAction.Pop

        /** Handles navigation to settings child screens */
        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}
