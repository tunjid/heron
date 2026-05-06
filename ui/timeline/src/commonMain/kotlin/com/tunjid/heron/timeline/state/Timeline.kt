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

import androidx.compose.runtime.Stable
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.TimelineRequest
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.launchTilingMutations
import com.tunjid.heron.tiling.reset
import com.tunjid.heron.tiling.updateItems
import com.tunjid.heron.tiling.withRefreshedStatus
import com.tunjid.heron.ui.coroutines.launchAndCollectLatest
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
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
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Transient

@Stable
@Snapshottable
interface TimelineState : TilingState<TimelineQuery, TimelineItem> {

    @SnapshotSpec
    data class Immutable(
        val timeline: Timeline,
        val hasUpdates: Boolean,
        @Transient
        override val tilingData: TilingState.Data<TimelineQuery, TimelineItem>,
    ) : TimelineState

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

fun TimelineState(
    timeline: Timeline,
    hasUpdates: Boolean,
    tilingData: TilingState.Data<TimelineQuery, TimelineItem>,
): TimelineState = TimelineState.SnapshotMutable(
    timeline = timeline,
    hasUpdates = hasUpdates,
    tilingData = tilingData,
)

typealias TimelineStateHolder = ActionSuspendingStateMutator<TimelineState.Action, TimelineState>

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
    return actionSuspendingStateMutator(
        initialState = TimelineState.SnapshotMutable(
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
        producer = { state, actions ->
            hasUpdatesMutations(
                state = state,
                timeline = timeline,
                timelineRepository = timelineRepository,
            )
            timelineUpdateMutations(
                state = state,
                timeline = timeline,
                timelineRepository = timelineRepository,
            )
            actions.launchMutationsIn(this) {
                when (val action = type()) {
                    is TimelineState.Action.Tile ->
                        action.flow
                            .map { it.tilingAction }
                            .launchTilingMutations(
                                isRefreshedOnNewItems = false,
                                state = state,
                                updateQueryData = TimelineQuery::updateData,
                                refreshQuery = TimelineQuery::refresh,
                                cursorListLoader = timelineRepository::timelineItems,
                                onNewItems = TiledList<TimelineQuery, TimelineItem>::filterThreadDuplicates,
                            )

                    is TimelineState.Action.UpdatePreferredPresentation ->
                        action.flow.updatePreferredPresentationMutations(
                            timelineRepository = timelineRepository,
                        )

                    is TimelineState.Action.DismissRefresh ->
                        action.flow.collect {
                            state.tilingData = state.tilingData.withRefreshedStatus()
                        }
                }
            }
        },
    )
}

context(scope: CoroutineScope)
private fun hasUpdatesMutations(
    state: TimelineState.SnapshotMutable,
    timeline: Timeline,
    timelineRepository: TimelineRepository,
) {
    timelineRepository.hasUpdates(timeline)
        .launchAndCollectLatest { updatesAvailable ->
            state.hasUpdates = updatesAvailable
            if (!updatesAvailable) {
                state.tilingData = state.tilingData.withRefreshedStatus()
            }
        }
}

context(scope: CoroutineScope)
private fun timelineUpdateMutations(
    state: TimelineState.SnapshotMutable,
    timeline: Timeline,
    timelineRepository: TimelineRepository,
) {
    timelineRepository.timeline(request = timeline.toTimelineRequest())
        .launchAndCollectLatest { newTimeline ->
            state.timeline = newTimeline

            if (newTimeline.isEmpty()) {
                delay(EMPTY_STATE_DELAY)
                if (state.timeline.isEmpty()) state.updateItems {
                    buildTiledList {
                        add(
                            query = state.tilingData.currentQuery,
                            item = TimelineItem.Empty.Timeline(state.timeline),
                        )
                    }
                }
            }
        }
}

context(scope: CoroutineScope)
private fun Flow<TimelineState.Action.UpdatePreferredPresentation>.updatePreferredPresentationMutations(
    timelineRepository: TimelineRepository,
) = launchAndCollectLatest {
    timelineRepository.updatePreferredPresentation(
        timeline = it.timeline,
        presentation = it.presentation,
    )
}

private fun Timeline.toTimelineRequest(): TimelineRequest = when (this) {
    is Timeline.Home.Feed -> TimelineRequest.OfFeed.WithUri(uri = feedGenerator.uri)
    is Timeline.Home.Following -> TimelineRequest.Following
    is Timeline.Home.List -> TimelineRequest.OfList.WithUri(uri = feedList.uri)
    is Timeline.Profile -> TimelineRequest.OfProfile(
        profileHandleOrDid = profileId,
        type = type,
    )
    is Timeline.StarterPack -> TimelineRequest.OfStarterPack.WithUri(uri = starterPack.uri)
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
    val threadRootIds = mutableSetOf<PostUri>()
    // The idea here, is to ensure that a particular root item
    // in a timeline only shows up once.
    return filter { item ->
        when (item) {
            is TimelineItem.Pinned -> true
            is TimelineItem.Threaded.Linear,
            -> threadRootIds.add(item.nodes.first().post.uri)
            is TimelineItem.Repost,
            is TimelineItem.Single,
            -> threadRootIds.add(item.post.uri)
            is TimelineItem.Placeholder,
            // Threaded trees show up in post details, not regular timelines
            is TimelineItem.Threaded.Tree,
            -> false
        }
    }
        .distinctBy(TimelineItem::id)
}

private val EMPTY_STATE_DELAY = 3.seconds
