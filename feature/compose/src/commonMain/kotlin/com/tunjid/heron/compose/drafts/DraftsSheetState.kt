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

package com.tunjid.heron.compose.drafts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.timeline.ui.TimeDelta
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.retainSheetStateHolder
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature.compose.generated.resources.Res
import heron.feature.compose.generated.resources.draft_delete
import heron.feature.compose.generated.resources.draft_media_count
import heron.feature.compose.generated.resources.draft_untitled
import heron.feature.compose.generated.resources.drafts_empty_description
import heron.feature.compose.generated.resources.drafts_empty_title
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

@Composable
fun PaneScaffoldState.rememberDraftsSheetState(
    onDraftSelected: (Post.Draft) -> Unit,
): DraftsSheetState =
    DraftsSheetState.rememberUpdatedDraftsSheetState(
        stateHolder = retainSheetStateHolder<DraftsStateHolder>(),
        onDraftSelected = onDraftSelected,
    )

@Stable
class DraftsSheetState internal constructor(
    scope: BottomSheetScope,
    internal val stateHolder: DraftsStateHolder,
) : BottomSheetState(scope) {

    fun showDrafts() {
        // Force a refresh so the list is populated (and reconciled with the stash) each time the
        // sheet is opened.
        stateHolder(DraftsAction.Tile(TilingState.Action.Refresh))
        show()
    }

    override fun onHidden() = Unit

    companion object {
        @Composable
        fun rememberUpdatedDraftsSheetState(
            stateHolder: DraftsStateHolder,
            onDraftSelected: (Post.Draft) -> Unit,
        ): DraftsSheetState {
            val state = rememberBottomSheetState(
                skipPartiallyExpanded = false,
                stateHolder = stateHolder,
                block = ::DraftsSheetState,
            )
            DraftsBottomSheet(
                state = state,
                onDraftSelected = onDraftSelected,
            )
            return state
        }
    }
}

@Composable
private fun DraftsBottomSheet(
    state: DraftsSheetState,
    onDraftSelected: (Post.Draft) -> Unit,
) {
    state.ModalBottomSheet {
        val draftsState = state.stateHolder.produceState()
        val listState = rememberLazyListState()

        if (draftsState.tiledItems.isEmpty()) EmptyContent(
            modifier = Modifier.navigationBarsPadding(),
            titleRes = Res.string.drafts_empty_title,
            descriptionRes = Res.string.drafts_empty_description,
            icon = Icons.Rounded.Drafts,
        )
        else LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = draftsState.tiledItems,
                key = { draft -> draft.id?.id ?: draft.hashCode().toString() },
            ) { draft ->
                DraftItem(
                    draft = draft,
                    onClick = {
                        onDraftSelected(draft)
                        state.hide()
                    },
                    onDelete = {
                        draft.id?.let { state.stateHolder(DraftsAction.Delete(it)) }
                    },
                )
            }
        }

        listState.PivotedTilingEffect(
            items = draftsState.tiledItems,
            onQueryChanged = { query ->
                state.stateHolder(
                    DraftsAction.Tile(
                        TilingState.Action.LoadAround(
                            query = query ?: draftsState.tilingData.currentQuery,
                        ),
                    ),
                )
            },
        )
    }
}

@Composable
private fun DraftItem(
    draft: Post.Draft,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val firstPost = draft.posts.firstOrNull()
            val mediaCount = firstPost?.metadata?.embeddedMedia?.size ?: 0
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                draft.createdAt?.let {
                    val now = remember { Clock.System.now() }
                    TimeDelta(
                        delta = now - it,
                    )
                }
                val text = firstPost?.text?.trim().orEmpty()
                Text(
                    text = text.ifBlank { stringResource(Res.string.draft_untitled) },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (mediaCount > 0) Text(
                    text = stringResource(Res.string.draft_media_count, mediaCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onDelete,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(Res.string.draft_delete),
                )
            }
        }
    }
}
