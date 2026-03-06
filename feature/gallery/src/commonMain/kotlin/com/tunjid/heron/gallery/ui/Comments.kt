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

package com.tunjid.heron.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.path
import com.tunjid.heron.gallery.Action
import com.tunjid.heron.scaffold.navigation.NavigationAction
import com.tunjid.heron.scaffold.navigation.composePostDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.UiTokens
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberCommentsState(): CommentsState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        saver = listSaver(
            save = {
                listOf(
                    it.height.toInt(),
                    it.anchoredDraggableState.currentValue.ordinal,
                )
            },
            restore = { (height, ordinal) ->
                CommentsState(
                    scope = scope,
                    height = height,
                    initialAnchor = Anchor.entries.firstOrNull { it.ordinal == ordinal },
                )
            },
        ),
    ) {
        CommentsState(scope)
    }
}

class CommentsState internal constructor(
    private val scope: CoroutineScope,
    height: Int = 0,
    initialAnchor: Anchor? = null,
) {
    var height by mutableFloatStateOf(height.toFloat())

    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialAnchor ?: Anchor.Collapsed,
        anchors = currentDraggableAnchors(),
    )

    fun expand() {
        scope.launch {
            anchoredDraggableState.animateTo(Anchor.Halfway)
        }
    }

    fun collapse() {
        if (anchoredDraggableState.currentValue != Anchor.Collapsed) scope.launch {
            anchoredDraggableState.animateTo(Anchor.Collapsed)
        }
    }
}

@Composable
fun Comments(
    paneScaffoldState: PaneScaffoldState,
    state: CommentsState,
    modifier: Modifier = Modifier,
    comments: List<TimelineItem>,
    actions: (Action) -> Unit,
) {
    val navigateTo = remember(actions) {
        { destination: NavigationAction.Destination ->
            actions(Action.Navigate.To(destination))
        }
    }
    Box(
        modifier = modifier
            .statusBarsPadding()
            .onSizeChanged { state.updateHeight(it.height) }
            .nestedScroll(state.nestedScrollConnection()),
    ) {
        val now = remember { Clock.System.now() }

        val postInteractionSheetState = rememberUpdatedPostInteractionsSheetState(
            isSignedIn = paneScaffoldState.isSignedIn,
            onSignInClicked = {
                actions(Action.Navigate.To(signInDestination()))
            },
            onInteractionConfirmed = {
                actions(Action.SendPostInteraction(it))
            },
            onQuotePostClicked = { repost ->
                navigateTo(
                    composePostDestination(
                        type = Post.Create.Quote(repost),
                        sharedElementPrefix = CommentSharedElementPrefix,
                    ),
                )
            },
        )

        ElevatedCard(
            shape = TopShape,
            modifier = Modifier
                .fillMaxSize()
                .offset { state.contentOffset }
                .anchoredDraggable(
                    enabled = false,
                    reverseDirection = true,
                    state = state.anchoredDraggableState,
                    orientation = Orientation.Vertical,
                ),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .height(2.dp)
                        .width(48.dp),
                )

                LookaheadScope {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = UiTokens.bottomNavAndInsetPaddingValues(
                            isCompact = paneScaffoldState.prefersCompactBottomNav,
                        ),
                        userScrollEnabled = !paneScaffoldState.isTransitionActive,
                    ) {
                        items(
                            items = comments,
                            key = TimelineItem::id,
                            itemContent = { item ->
                                TimelineItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(),
                                    paneMovableElementSharedTransitionScope = paneScaffoldState,
                                    presentationLookaheadScope = this@LookaheadScope,
                                    now = now,
                                    item = item,
                                    sharedElementPrefix = CommentSharedElementPrefix,
                                    showEngagementMetrics = false,
                                    presentation = Timeline.Presentation.Text.WithEmbed,
                                    postActions = remember(Unit) {
                                        PostActions { action ->
                                            when (action) {
                                                is PostAction.OfLinkTarget -> {
                                                    val linkTarget = action.linkTarget
                                                    if (linkTarget is LinkTarget.Navigable) navigateTo(
                                                        pathDestination(
                                                            path = linkTarget.path,
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                        ),
                                                    )
                                                }

                                                is PostAction.OfPost -> {
                                                    navigateTo(
                                                        recordDestination(
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                                            sharedElementPrefix = CommentSharedElementPrefix,
                                                            otherModels = listOfNotNull(
                                                                action.warnedAppliedLabels,
                                                            ),
                                                            record = action.post,
                                                        ),
                                                    )
                                                }

                                                is PostAction.OfProfile -> {
                                                    navigateTo(
                                                        profileDestination(
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Current,
                                                            profile = action.profile,
                                                            avatarSharedElementKey = action.post.avatarSharedElementKey(
                                                                prefix = CommentSharedElementPrefix,
                                                                quotingPostUri = action.quotingPostUri,
                                                            )
                                                                .takeIf { action.post.author.did == action.profile.did },
                                                        ),
                                                    )
                                                }
                                                is PostAction.OfReply -> {
                                                    navigateTo(
                                                        if (paneScaffoldState.isSignedOut) signInDestination()
                                                        else composePostDestination(
                                                            type = Post.Create.Reply(
                                                                parent = action.post,
                                                            ),
                                                            sharedElementPrefix = CommentSharedElementPrefix,
                                                        ),
                                                    )
                                                }
                                                is PostAction.OfInteraction -> {
                                                    postInteractionSheetState.onInteraction(action)
                                                }
                                                is PostAction.OfRecord,
                                                is PostAction.OfMedia,
                                                is PostAction.OfMetadata,
                                                is PostAction.OfMore,
                                                -> Unit
                                            }
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

private val CommentsState.contentOffset
    get() = (height - anchoredDraggableState.requireOffset())
        .toOffset()
        .round()

private val CommentsState.progress: Float get() = anchoredDraggableState.requireOffset() / height

private fun CommentsState.updateHeight(height: Int) {
    this.height = height.toFloat()
    anchoredDraggableState.updateAnchors(currentDraggableAnchors())
}

private fun CommentsState.currentDraggableAnchors() = DraggableAnchors {
    Anchor.Collapsed at 0f
    Anchor.Halfway at height / 2
    Anchor.Expanded at height
}

private fun CommentsState.nestedScrollConnection() =
    object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset = when (val delta = available.y) {
            in -Float.MAX_VALUE..-Float.MIN_VALUE -> anchoredDraggableState.dispatchInvertedDelta(
                delta,
            )
                .toOffset()

            else -> Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset =
            anchoredDraggableState.dispatchInvertedDelta(delta = available.y).toOffset()

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity,
        ): Velocity {
            val currentValue = anchoredDraggableState.currentValue

            if (available.y > 0 && currentValue == Anchor.Expanded) {
                return animateToStatusWithVelocity(
                    available = available,
                    anchor = Anchor.Collapsed,
                )
            }
            if (available.y < 0 && currentValue == Anchor.Collapsed) {
                return animateToStatusWithVelocity(
                    available = available,
                    anchor = Anchor.Expanded,
                )
            }

            val hasNoInertia = available == Velocity.Zero && consumed == Velocity.Zero

            if (hasNoInertia && progress < ProgressThreshold) {
                return animateToStatusWithVelocity(
                    available = available,
                    anchor = Anchor.Collapsed,
                )
            }
            if (hasNoInertia && progress > 1 - ProgressThreshold) {
                return animateToStatusWithVelocity(
                    available = available,
                    anchor = Anchor.Expanded,
                )
            }

            return super.onPostFling(consumed, available)
        }

        private suspend fun animateToStatusWithVelocity(
            available: Velocity,
            anchor: Anchor,
        ) = Velocity(
            x = 0f,
            y = -anchoredDraggableState.animateToWithDecay(
                targetValue = anchor,
                velocity = -available.y,
            ),
        )
    }

private fun AnchoredDraggableState<*>.dispatchInvertedDelta(
    delta: Float,
) = -dispatchRawDelta(-delta)

private fun Float.toOffset() = Offset(0f, this)

private val TopShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
)

internal enum class Anchor {
    Collapsed,
    Halfway,
    Expanded,
}

private const val CommentSharedElementPrefix = "gallery-comment-shared-element-prefix"

private const val ProgressThreshold = 0.5f
