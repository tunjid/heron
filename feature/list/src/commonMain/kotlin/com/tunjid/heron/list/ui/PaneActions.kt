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

package com.tunjid.heron.list.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.uri
import com.tunjid.heron.data.core.types.asEmbeddableRecordUriOrNull
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.list.Action
import com.tunjid.heron.list.State
import com.tunjid.heron.list.timelineState
import com.tunjid.heron.list.withFeedListOrNull
import com.tunjid.heron.sheets.rememberEmbeddableRecordOptionsSheetState
import com.tunjid.heron.timeline.ui.ShareRecordAppBarButton
import com.tunjid.heron.timeline.ui.list.FeedListStatus
import com.tunjid.heron.timeline.utilities.TimelineStrings
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.GroupAdd
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.list.generated.resources.Res
import heron.feature.list.generated.resources.follow_starter_pack
import heron.ui.core.generated.resources.record_list
import heron.ui.core.generated.resources.record_starter_pack
import heron.ui.timeline.generated.resources.share_record
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
    actions: (Action) -> Unit,
) {
    val recordOptionsSheetState = rememberEmbeddableRecordOptionsSheetState(
        editTitle = null,
        onEditClicked = {},
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
        onShareInPostClicked = { recordUri ->
            actions(
                Action.Navigate.To(
                    composePostDestination(
                        sharedUri = recordUri.asGenericUri(),
                    ),
                ),
            )
        },
    )

    val timeline = state.timelineState?.timeline
    timeline?.withFeedListOrNull { feedList ->
        FeedListStatus(
            status = state.listStatus,
            uri = feedList.uri,
            onListStatusUpdated = {
                actions(Action.UpdateFeedListStatus(it))
            },
        )
    }
    if (timeline is Timeline.StarterPack) {
        AppBarIconButton(
            icon = HeronIcons.Regular.GroupAdd,
            iconDescription = stringResource(Res.string.follow_starter_pack),
            onClick = click@{
                actions(
                    Action.FollowStarterPack(
                        signedInProfileId = state.signedInProfileId ?: return@click,
                        starterPackUri = timeline.starterPack.uri,
                        starterPackCid = timeline.starterPack.cid,
                        listUri = timeline.starterPack.list?.uri ?: return@click,
                    ),
                )
            },
        )
    }
    ShareRecordAppBarButton(
        contentDescription = stringResource(
            TimelineStrings.share_record,
            stringResource(
                if (timeline is Timeline.StarterPack) CommonStrings.record_starter_pack
                else CommonStrings.record_list,
            ),
        ),
        onShareClicked = {
            state.timelineState?.timeline?.uri
                ?.asEmbeddableRecordUriOrNull()
                ?.let { recordUri ->
                    recordOptionsSheetState.showOptions(recordUri)
                }
        },
    )
}
