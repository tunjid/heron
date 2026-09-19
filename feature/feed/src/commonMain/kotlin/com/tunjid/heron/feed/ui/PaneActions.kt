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

package com.tunjid.heron.feed.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.graze.isGrazeFeed
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.feed.Action
import com.tunjid.heron.feed.State
import com.tunjid.heron.feed.timelineState
import com.tunjid.heron.feed.withFeedTimelineOrNull
import com.tunjid.heron.sheets.rememberEmbeddableRecordOptionsSheetState
import com.tunjid.heron.timeline.ui.ShareRecordAppBarButton
import com.tunjid.heron.timeline.ui.feed.FeedGeneratorStatus
import com.tunjid.heron.timeline.utilities.TimelineStrings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.navigation.grazeEditorDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.feed.generated.resources.Res
import heron.feature.feed.generated.resources.edit_feed
import heron.ui.core.generated.resources.record_feed
import heron.ui.timeline.generated.resources.share_record
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
    actions: (Action) -> Unit,
) {
    val editFeedText = stringResource(Res.string.edit_feed)
    val recordOptionsSheetState = rememberEmbeddableRecordOptionsSheetState(
        editTitle = state.timelineState?.timeline?.withFeedTimelineOrNull { timeline ->
            val isEditable = timeline.feedGenerator.isGrazeFeed &&
                state.signedInProfileId == timeline.feedGenerator.creator.did
            if (isEditable) editFeedText else null
        },
        onShareInConversationClicked = { recordUri, conversation ->
            actions(
                Action.Navigate.To(
                    conversationDestination(
                        id = conversation.id,
                        members = conversation.members,
                        sharedElementPrefix = conversation.id.id,
                        sharedUri = recordUri.asGenericUri(),
                        referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                    ),
                ),
            )
        },
        onEditClicked = onEditClicked@{
            actions(
                Action.Navigate.To(
                    grazeEditorDestination(
                        feedGenerator = state.timelineState
                            ?.timeline
                            ?.withFeedTimelineOrNull(Timeline.Home.Feed::feedGenerator)
                            ?: return@onEditClicked,
                        sharedElementPrefix = state.sharedElementPrefix,
                    ),
                ),
            )
        },
        onShareInPostClicked = { recordUri ->
            actions(
                Action.Navigate.To(
                    composePostDestination(sharedUri = recordUri.asGenericUri()),
                ),
            )
        },
    )

    state.timelineState
        ?.timeline
        ?.withFeedTimelineOrNull { feedTimeline ->
            FeedGeneratorStatus(
                status = state.feedStatus,
                uri = feedTimeline.feedGenerator.uri,
                onFeedGeneratorStatusUpdated = {
                    actions(Action.UpdateFeedGeneratorStatus(it))
                },
            )
        }
    // Only record-backed timelines (feeds) can be shared; topic timelines have no uri.
    state.timelineState
        ?.timeline
        ?.uri
        ?.asEmbeddableRecordUriOrNull()
        ?.let { recordUri ->
            ShareRecordAppBarButton(
                contentDescription = stringResource(
                    TimelineStrings.share_record,
                    stringResource(CommonStrings.record_feed),
                ),
                onShareClicked = {
                    recordOptionsSheetState.showOptions(recordUri)
                },
            )
        }
}
