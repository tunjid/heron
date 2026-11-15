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

package com.tunjid.heron.externalrecord.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.utilities.asGenericUri
import com.tunjid.heron.externalrecord.Action
import com.tunjid.heron.externalrecord.State
import com.tunjid.heron.sheets.rememberEmbeddableRecordOptionsSheetState
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.ShareRecordAppBarButton
import com.tunjid.heron.timeline.utilities.TimelineStrings
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.composePostDestination
import com.tunjid.heron.ui.scaffold.navigation.conversationDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.record_event
import heron.ui.timeline.generated.resources.share_record
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
    actions: (Action) -> Unit,
) {
    when (val record = state.record) {
        is StandardPublication -> SubscribeButton(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            publication = record,
            onSubscriptionToggled = { publication, _ ->
                actions(
                    Action.RecordInteraction(
                        interaction = PostAction.OfPublicationSubscription(
                            publication = publication,
                        ),
                    ),
                )
            },
        )
        is Event -> {
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
            ShareRecordAppBarButton(
                contentDescription = stringResource(
                    TimelineStrings.share_record,
                    stringResource(CommonStrings.record_event),
                ),
                onShareClicked = {
                    recordOptionsSheetState.showOptions(record.uri)
                },
            )
        }
        else -> Unit
    }
}
