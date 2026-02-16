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
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.tilingMutations
import com.tunjid.heron.tiling.withRefreshedStatus
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapLatestToManyMutations
import com.tunjid.mutator.coroutines.mapLatestToMutation
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.tiler.TiledList
import com.tunjid.tiler.buildTiledList
import com.tunjid.tiler.distinctBy
import com.tunjid.tiler.emptyTiledList
import com.tunjid.tiler.filter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

data class TimelineState(
    val timeline: Timeline,
    val hasUpdates: Boolean,
    override val tilingData: TilingState.Data<TimelineQuery, TimelineItem>,
) : TilingState<TimelineQuery, TimelineItem> {
    sealed class Action(
        val key: String,
    ) {
        data class Tile(
            val tilingAction: TilingState.Action,
        ) : Action(key = "Tile")

        data class UpdatePreferredPresentation(
            val timeline: Timeline,
            val presentation: Timeline.Presentation,
        ) : Action(key = "UpdatePreferredPresentation")

        data object DismissRefresh : Action(key = "DismissRefresh")
    }
}

typealias TimelineStateHolder = ActionStateMutator<TimelineState.Action, StateFlow<TimelineState>>

fun CoroutineScope.timelineStateHolder(
    refreshOnStart: Boolean,
    timeline: Timeline,
    startNumColumns: Int,
    initialItems: List<TimelineItem> = emptyList(),
    timelineRepository: TimelineRepository,
): TimelineStateHolder {
    val initialQuery = TimelineQuery(
        source = timeline.source,
        data = CursorQuery.Data(
            page = 0,
            cursorAnchor = when (timeline) {
                is Timeline.Home,
                is Timeline.StarterPack,
                ->
                    timeline.lastRefreshed
                        .takeUnless { refreshOnStart }
                        ?: Clock.System.now()

                is Timeline.Profile -> Clock.System.now()
            },
        ),
    )
    return actionStateFlowMutator(
        initialState = TimelineState(
            timeline = timeline,
            hasUpdates = false,
            tilingData = TilingState.Data(
                items =
                if (initialItems.isEmpty()) emptyTiledList()
                else buildTiledList { addAll(initialQuery, initialItems) },
                numColumns = startNumColumns,
                currentQuery = initialQuery,
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
            actions.toMutationStream(keySelector = TimelineState.Action::key) {
                when (val action = type()) {
                    is TimelineState.Action.Tile ->
                        action.flow
                            .map { it.tilingAction }
                            .tilingMutations(
                                // This is determined by State.hasUpdates
                                isRefreshedOnNewItems = false,
                                currentState = { this@transform.state() },
                                updateQueryData = TimelineQuery::updateData,
                                refreshQuery = TimelineQuery::refresh,
                                cursorListLoader = timelineRepository::timelineItems,
                                onNewItems = TiledList<TimelineQuery, TimelineItem>::filterThreadDuplicates,
                                onTilingDataUpdated = { copy(tilingData = it) },
                            )

                    is TimelineState.Action.UpdatePreferredPresentation -> action.flow.updatePreferredPresentationMutations(
                        timelineRepository = timelineRepository,
                    )
                    is TimelineState.Action.DismissRefresh -> action.flow.dismissRefreshMutations()
                }
            }
        },
    )
}

private fun hasUpdatesMutations(
    timeline: Timeline,
    timelineRepository: TimelineRepository,
): Flow<Mutation<TimelineState>> =
    timelineRepository.hasUpdates(timeline)
        .mapToMutation { updatesAvailable ->
            copy(
                hasUpdates = updatesAvailable,
                tilingData = if (updatesAvailable) tilingData else tilingData.withRefreshedStatus(),
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
        },
    )
        .mapLatestToManyMutations { newTimeline ->
            emit { copy(timeline = newTimeline) }

            if (newTimeline.isEmpty()) {
                delay(2.seconds)
                if (timeline.isEmpty()) emit {
                    copy(
                        tilingData = tilingData.copy(
                            items = buildTiledList {
                                add(
                                    query = tilingData.currentQuery,
                                    item = TimelineItem.Empty(timeline),
                                )
                            },
                        ),
                    )
                }
            }
        }

private fun Flow<TimelineState.Action.UpdatePreferredPresentation>.updatePreferredPresentationMutations(
    timelineRepository: TimelineRepository,
): Flow<Mutation<TimelineState>> = mapLatestToManyMutations {
    timelineRepository.updatePreferredPresentation(
        timeline = it.timeline,
        presentation = it.presentation,
    )
}

private fun Flow<TimelineState.Action.DismissRefresh>.dismissRefreshMutations(): Flow<Mutation<TimelineState>> =
    mapLatestToMutation {
        copy(
            tilingData = tilingData.withRefreshedStatus(),
        )
    }

private fun Timeline.isEmpty(): Boolean =
    itemsAvailable == 0L && lastRefreshed != null

private fun TimelineQuery.updateData(
    data: CursorQuery.Data,
): TimelineQuery = copy(
    data = data,
)

private fun TimelineQuery.refresh(): TimelineQuery = copy(
    data = data.reset(),
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
            is TimelineItem.Placeholder -> false
        }
    }
        .distinctBy(TimelineItem::id)
}
