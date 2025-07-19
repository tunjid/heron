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

package com.tunjid.heron.timeline.state

import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.filter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

data class TimelineState(
    val timeline: Timeline,
    val hasUpdates: Boolean,
    override val tilingData: TilingState.Data<TimelineQuery, TimelineItem>,
) : TilingState<TimelineQuery, TimelineItem>

sealed class TimelineLoadAction(
    val key: String,
) {

    data class Tile(
        val tilingAction: TilingState.Action,
    ) : TimelineLoadAction(key = "Fetch")

    data class UpdatePreferredPresentation(
        val timeline: Timeline,
        val presentation: Timeline.Presentation,
    ) : TimelineLoadAction(key = "UpdatePreferredPresentation")
}

typealias TimelineStateHolder = ActionStateMutator<TimelineLoadAction, StateFlow<TimelineState>>

fun timelineStateHolder(
    refreshOnStart: Boolean,
    timeline: Timeline,
    startNumColumns: Int,
    scope: CoroutineScope,
    timelineRepository: TimelineRepository,
): TimelineStateHolder = scope.actionStateFlowMutator(
    initialState = TimelineState(
        timeline = timeline,
        hasUpdates = false,
        tilingData = TilingState.Data(
            numColumns = startNumColumns,
            currentQuery = TimelineQuery(
                timeline = timeline,
                data = CursorQuery.Data(
                    page = 0,
                    cursorAnchor = when (timeline) {
                        is Timeline.Home,
                        is Timeline.StarterPack -> timeline.lastRefreshed
                            .takeUnless { refreshOnStart }
                            ?: Clock.System.now()

                        is Timeline.Profile -> Clock.System.now()
                    },
                ),
            ),
        ),
    ),
    inputs = listOf(
        hasUpdatesMutations(
            timeline = timeline,
            timelineRepository = timelineRepository,
        ),
        timelineUpdateMutations(
            timeline = timeline,
            timelineRepository = timelineRepository,
        ),
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(keySelector = TimelineLoadAction::key) {
            when (val action = type()) {
                is TimelineLoadAction.Tile -> action.flow
                    .map { it.tilingAction }
                    .tilingMutations(
                        currentState = { state() },
                        onRefreshQuery = TimelineQuery::refresh,
                        updatePage = TimelineQuery::updateData,
                        cursorListLoader = timelineRepository::timelineItems,
                        onNewItems = TiledList<TimelineQuery, TimelineItem>::filterThreadDuplicates,
                        onTilingDataUpdated = { copy(tilingData = it) },
                    )

                is TimelineLoadAction.UpdatePreferredPresentation -> action.flow.updatePreferredPresentationMutations(
                    timelineRepository = timelineRepository,
                )
            }
        }
    }
)

private fun hasUpdatesMutations(
    timeline: Timeline,
    timelineRepository: TimelineRepository,
): Flow<Mutation<TimelineState>> =
    timelineRepository.hasUpdates(timeline)
        .mapToMutation {
            copy(
                hasUpdates = it,
                tilingData = if (hasUpdates) tilingData else tilingData.copy(
                    status = TilingState.Status.Refreshed(
                        cursorAnchor = tilingData.currentQuery.data.cursorAnchor
                    )
                ),
            )
        }

private fun timelineUpdateMutations(
    timeline: Timeline,
    timelineRepository: TimelineRepository,
): Flow<Mutation<TimelineState>> =
    timelineRepository.timeline(
        request = when (timeline) {
            is Timeline.Home.Feed -> TimelineRequest.OfFeed.WithUri(
                uri = timeline.feedGenerator.uri,
            )

            is Timeline.Home.Following -> TimelineRequest.Following

            is Timeline.Home.List -> TimelineRequest.OfList.WithUri(
                uri = timeline.feedList.uri,
            )

            is Timeline.Profile -> TimelineRequest.OfProfile(
                profileHandleOrDid = timeline.profileId,
                type = timeline.type,
            )

            is Timeline.StarterPack -> TimelineRequest.OfStarterPack.WithUri(
                uri = timeline.starterPack.uri,
            )
        }
    )
        .mapToMutation { copy(timeline = it) }

private suspend fun Flow<TimelineLoadAction.UpdatePreferredPresentation>.updatePreferredPresentationMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<TimelineState>> = mapLatestToManyMutations {
    timelineRepository.updatePreferredPresentation(
        timeline = it.timeline,
        presentation = it.presentation,
    )
}

private fun TimelineQuery.updateData(
    data: CursorQuery.Data
): TimelineQuery = TimelineQuery(
    timeline = timeline,
    data = data,
)

private fun TimelineQuery.refresh(): TimelineQuery = TimelineQuery(
    timeline = timeline,
    data = CursorQuery.Data(
        page = 0,
        cursorAnchor = Clock.System.now(),
    ),
)

private fun TiledList<TimelineQuery, TimelineItem>.filterThreadDuplicates(): TiledList<TimelineQuery, TimelineItem> {
    val threadRootIds = mutableSetOf<PostId>()
    return filter { item ->
        when (item) {
            is TimelineItem.Pinned -> true
            is TimelineItem.Thread -> !threadRootIds.contains(item.posts.first().cid)
                .also { contains ->
                    if (!contains) threadRootIds.add(item.posts.first().cid)
                }

            is TimelineItem.Repost -> !threadRootIds.contains(item.post.cid).also { contains ->
                if (!contains) threadRootIds.add(item.post.cid)
            }

            is TimelineItem.Single -> !threadRootIds.contains(item.post.cid)
        }
    }
        .distinctBy(TimelineItem::id)
}
