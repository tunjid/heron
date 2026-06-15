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

package com.tunjid.heron.timeline.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.RockskyAlbum
import com.tunjid.heron.data.core.models.RockskyArtist
import com.tunjid.heron.data.core.models.RockskyScrobble
import com.tunjid.heron.data.core.models.RockskyTrack
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.ui.UiTokens
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.empty_records
import heron.ui.timeline.generated.resources.empty_records_albums
import heron.ui.timeline.generated.resources.empty_records_albums_description
import heron.ui.timeline.generated.resources.empty_records_artists
import heron.ui.timeline.generated.resources.empty_records_artists_description
import heron.ui.timeline.generated.resources.empty_records_description
import heron.ui.timeline.generated.resources.empty_records_documents
import heron.ui.timeline.generated.resources.empty_records_documents_description
import heron.ui.timeline.generated.resources.empty_records_feeds
import heron.ui.timeline.generated.resources.empty_records_feeds_description
import heron.ui.timeline.generated.resources.empty_records_lists
import heron.ui.timeline.generated.resources.empty_records_lists_description
import heron.ui.timeline.generated.resources.empty_records_publications
import heron.ui.timeline.generated.resources.empty_records_publications_description
import heron.ui.timeline.generated.resources.empty_records_scrobbles
import heron.ui.timeline.generated.resources.empty_records_scrobbles_description
import heron.ui.timeline.generated.resources.empty_records_starter_packs
import heron.ui.timeline.generated.resources.empty_records_starter_packs_description
import heron.ui.timeline.generated.resources.empty_records_tracks
import heron.ui.timeline.generated.resources.empty_records_tracks_description
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.StringResource

@Composable
inline fun <reified T : Record, State : TilingState<out CursorQuery, T>> RecordList(
    collectionStateHolder: ActionSuspendingStateMutator<TilingState.Action, State>,
    prefersCompactBottomNav: Boolean,
    emptyTitleRes: StringResource = T::class.emptyTitleRes,
    emptyDescriptionRes: StringResource = T::class.emptyDescriptionRes,
    emptyIcon: ImageVector = T::class.emptyIcon,
    crossinline itemKey: (T) -> Any,
    crossinline itemContent: @Composable LazyItemScope.(T) -> Unit,
) = RecordList(
    collectionStateHolder = collectionStateHolder,
    prefersCompactBottomNav = prefersCompactBottomNav,
    emptyTitleRes = emptyTitleRes,
    emptyDescriptionRes = emptyDescriptionRes,
    emptyIcon = emptyIcon,
    itemContent = {
        items(
            items = it,
            key = { item -> itemKey(item) },
            itemContent = itemContent,
        )
    },
)

@Composable
inline fun <reified T : Record, State : TilingState<out CursorQuery, T>> RecordList(
    collectionStateHolder: ActionSuspendingStateMutator<TilingState.Action, State>,
    prefersCompactBottomNav: Boolean,
    emptyTitleRes: StringResource = T::class.emptyTitleRes,
    emptyDescriptionRes: StringResource = T::class.emptyDescriptionRes,
    emptyIcon: ImageVector = T::class.emptyIcon,
    crossinline itemContent: LazyListScope.(List<T>) -> Unit,
) {
    val listState = rememberLazyListState()
    val collectionState = collectionStateHolder.produceStateWithLifecycle()

    var isEmpty by rememberSaveable {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = listState,
        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
            horizontal = 8.dp,
            isCompact = prefersCompactBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemContent(collectionState.tiledItems)

        if (isEmpty) {
            item {
                EmptyContent(
                    titleRes = emptyTitleRes,
                    descriptionRes = emptyDescriptionRes,
                    icon = emptyIcon,
                )
            }
        }
    }

    listState.PivotedTilingEffect(
        items = collectionState.tiledItems,
        onQueryChanged = { query ->
            collectionStateHolder.accept(
                TilingState.Action.LoadAround(
                    query = query ?: collectionState.tilingData.currentQuery,
                ),
            )
        },
    )

    LaunchedEffect(Unit) {
        snapshotFlow {
            collectionState.tiledItems.isEmpty()
        }.collectLatest { currentlyEmpty ->
            if (currentlyEmpty) delay(3.seconds)
            isEmpty = currentlyEmpty
        }
    }
}

@PublishedApi
internal val KClass<out Record>.emptyTitleRes: StringResource
    get() = when (this) {
        FeedGenerator::class -> Res.string.empty_records_feeds
        StarterPack::class -> Res.string.empty_records_starter_packs
        FeedList::class -> Res.string.empty_records_lists
        StandardDocument::class -> Res.string.empty_records_documents
        StandardPublication::class -> Res.string.empty_records_publications
        RockskyAlbum::class -> Res.string.empty_records_albums
        RockskyTrack::class -> Res.string.empty_records_tracks
        RockskyArtist::class -> Res.string.empty_records_artists
        RockskyScrobble::class -> Res.string.empty_records_scrobbles
        else -> Res.string.empty_records
    }

@PublishedApi
internal val KClass<out Record>.emptyDescriptionRes: StringResource
    get() = when (this) {
        FeedGenerator::class -> Res.string.empty_records_feeds_description
        StarterPack::class -> Res.string.empty_records_starter_packs_description
        FeedList::class -> Res.string.empty_records_lists_description
        StandardDocument::class -> Res.string.empty_records_documents_description
        StandardPublication::class -> Res.string.empty_records_publications_description
        RockskyAlbum::class -> Res.string.empty_records_albums_description
        RockskyTrack::class -> Res.string.empty_records_tracks_description
        RockskyArtist::class -> Res.string.empty_records_artists_description
        RockskyScrobble::class -> Res.string.empty_records_scrobbles_description
        else -> Res.string.empty_records_description
    }

@PublishedApi
internal val KClass<out Record>.emptyIcon: ImageVector
    get() = when (this) {
        FeedGenerator::class -> Icons.Rounded.DynamicFeed
        StarterPack::class -> Icons.Rounded.Group
        FeedList::class -> Icons.AutoMirrored.Rounded.List
        StandardDocument::class -> Icons.AutoMirrored.Rounded.Article
        StandardPublication::class -> Icons.Rounded.Public
        RockskyAlbum::class -> Icons.Rounded.Album
        RockskyTrack::class -> Icons.Rounded.MusicNote
        RockskyArtist::class -> Icons.Rounded.Mic
        RockskyScrobble::class -> Icons.Rounded.History
        else -> Icons.Rounded.Inbox
    }
