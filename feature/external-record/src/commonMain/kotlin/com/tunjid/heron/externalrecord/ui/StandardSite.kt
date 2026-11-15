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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.data.core.models.link
import com.tunjid.heron.externalrecord.Action
import com.tunjid.heron.externalrecord.DocumentsStateHolder
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.tiling.TilingState
import com.tunjid.heron.tiling.tiledItems
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.standard.Document
import com.tunjid.heron.timeline.ui.standard.PublicationSubscriptionIcon
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.collectionShape
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.UiTokens.bottomNavAndInsetPaddingValues
import com.tunjid.heron.ui.modifiers.gridColumnCount
import com.tunjid.heron.ui.modifiers.shapedClickable
import com.tunjid.heron.ui.rememberLatchedState
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.scaffold.scaffold.paneClip
import com.tunjid.mutator.compose.produceStateWithLifecycle
import com.tunjid.tiler.compose.PivotedTilingEffect
import heron.feature.external_record.generated.resources.Res
import heron.feature.external_record.generated.resources.publication_publisher
import org.jetbrains.compose.resources.stringResource

@Composable
fun PublicationTitle(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String?,
    publication: StandardPublication?,
) = with(paneTransitionScope) {
    if (publication != null) Row(
        modifier = modifier
            .padding(
                horizontal = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaneStickySharedElement(
            modifier = Modifier
                .size(44.dp),
            sharedContentState = rememberSharedContentState(
                key = publication.avatarSharedElementKey(
                    prefix = sharedElementPrefix,
                ),
            ),
            zIndexInOverlay = UiTokens.higherThanAppBarSharedElementZIndex(),
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                args = remember(publication.icon) {
                    ImageArgs(
                        url = publication.icon?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = publication.collectionShape(),
                    )
                },
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f),
        ) {
            Column {
                Text(
                    modifier = Modifier,
                    text = publication.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    modifier = Modifier,
                    text = stringResource(
                        Res.string.publication_publisher,
                        publication.publisher.handle.id,
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
internal fun Documents(
    holder: DocumentsStateHolder,
    paneScaffoldState: PaneScaffoldState,
    actions: (Action) -> Unit,
) {
    val state = holder.produceStateWithLifecycle()
    val gridState = rememberLazyStaggeredGridState()

    LazyVerticalStaggeredGrid(
        modifier = Modifier
            .fillMaxSize()
            .paneClip()
            .gridColumnCount(LocalDensity.current) { numColumns ->
                holder.accept(
                    TilingState.Action.GridSize(numColumns = numColumns),
                )
            },
        state = gridState,
        columns = StaggeredGridCells.Adaptive(360.dp),
        verticalItemSpacing = 8.dp,
        contentPadding = bottomNavAndInsetPaddingValues(
            isCompact = paneScaffoldState.prefersCompactBottomNav,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = !paneScaffoldState.isTransitionActive,
    ) {
        items(
            items = state.tiledItems,
            key = { it.uri.uri },
            itemContent = { document ->
                val uriHandler = LocalUriHandler.current
                Document(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .animateItem()
                        .shapedClickable {
                            document.link?.let { link ->
                                runCatching { uriHandler.openUri(link) }
                            }
                        }
                        .padding(horizontal = 8.dp),
                    paneTransitionScope = paneScaffoldState,
                    sharedElementPrefix = StandardPublicationSharedElementPrefix,
                    document = document,
                    onPublicationClicked = null,
                    onSubscriptionToggled = null,
                    onRecommendationToggled = {
                        actions(
                            Action.RecordInteraction(
                                interaction = PostAction.OfDocumentRecommendation(
                                    document = it,
                                ),
                            ),
                        )
                    },
                )
            },
        )
    }
    gridState.PivotedTilingEffect(
        items = state.tiledItems,
        onQueryChanged = { query ->
            holder.accept(
                TilingState.Action.LoadAround(
                    query = query ?: state.tilingData.currentQuery,
                ),
            )
        },
    )
}

@Composable
fun SubscribeButton(
    modifier: Modifier = Modifier,
    publication: StandardPublication,
    onSubscriptionToggled: (StandardPublication, StandardSubscription?) -> Unit,
) {
    val subscribed = publication.subscription != null
    val latchedSubscribedState = rememberLatchedState(subscribed)

    AppBarIconButton(
        modifier = modifier,
        onClick = {
            latchedSubscribedState.latch(!subscribed)
            onSubscriptionToggled(
                publication,
                publication.subscription,
            )
        },
        content = {
            PublicationSubscriptionIcon(
                subscribed = latchedSubscribedState.value,
                iconSize = 24.dp,
            )
        },
    )
}

private const val StandardPublicationSharedElementPrefix = "standard-publication"
