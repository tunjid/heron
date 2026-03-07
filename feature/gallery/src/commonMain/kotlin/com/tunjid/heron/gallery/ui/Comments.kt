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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import com.tunjid.heron.scaffold.navigation.galleryDestination
import com.tunjid.heron.scaffold.navigation.pathDestination
import com.tunjid.heron.scaffold.navigation.profileDestination
import com.tunjid.heron.scaffold.navigation.recordDestination
import com.tunjid.heron.scaffold.navigation.signInDestination
import com.tunjid.heron.scaffold.scaffold.PaneFab
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.TimelineItem
import com.tunjid.heron.timeline.ui.post.PostInteractionsSheetState.Companion.rememberUpdatedPostInteractionsSheetState
import com.tunjid.heron.timeline.ui.post.PostOptionsSheetState
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.lazyGridVerticalItemSpacing
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.text.withFormattedTextPost
import heron.feature.gallery.generated.resources.Res
import heron.feature.gallery.generated.resources.reply_hint
import heron.feature.gallery.generated.resources.reply_send
import kotlin.math.max
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
        val shouldCollapse = anchoredDraggableState.currentValue != Anchor.Collapsed &&
            anchoredDraggableState.targetValue != Anchor.Collapsed
        if (shouldCollapse) scope.launch {
            anchoredDraggableState.animateTo(Anchor.Collapsed)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun Comments(
    paneScaffoldState: PaneScaffoldState,
    state: CommentsState,
    postOptionsSheetState: PostOptionsSheetState,
    modifier: Modifier = Modifier,
    comments: List<TimelineItem>,
    inputText: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendReply: () -> Unit,
    actions: (Action) -> Unit,
) {
    val commentSharedElementPrefix = rememberSaveable {
        Uuid.random().toString()
    }

    val navigateTo = remember(actions) {
        { destination: NavigationAction.Destination ->
            actions(Action.Navigate.To(destination))
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { state.updateHeight(it.height) }
            .nestedScroll(state.nestedScrollConnection()),
    ) {
        val now = remember { Clock.System.now() }
        val presentation = Timeline.Presentation.Text.WithEmbed

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
                        sharedElementPrefix = commentSharedElementPrefix,
                    ),
                )
            },
        )

        ElevatedCard(
            shape = TopShape,
            modifier = Modifier
                .offset(y = UiTokens.statusBarHeight)
                .offset { state.contentOffset }
                .fillMaxSize()
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
                        verticalArrangement = Arrangement.spacedBy(
                            presentation.lazyGridVerticalItemSpacing,
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
                                    sharedElementPrefix = commentSharedElementPrefix,
                                    showEngagementMetrics = false,
                                    presentation = presentation,
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
                                                            sharedElementPrefix = commentSharedElementPrefix,
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
                                                                prefix = commentSharedElementPrefix,
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
                                                            sharedElementPrefix = commentSharedElementPrefix,
                                                        ),
                                                    )
                                                }
                                                is PostAction.OfInteraction -> {
                                                    postInteractionSheetState.onInteraction(action)
                                                }
                                                is PostAction.OfRecord -> {
                                                    val record = action.record
                                                    val owningPostUri = action.owningPostUri
                                                    navigateTo(
                                                        recordDestination(
                                                            referringRouteOption = NavigationAction.ReferringRouteOption.Parent,
                                                            sharedElementPrefix = commentSharedElementPrefix.withQuotingPostUriPrefix(
                                                                quotingPostUri = owningPostUri,
                                                            ),
                                                            record = record,
                                                        ),
                                                    )
                                                }
                                                is PostAction.OfMedia -> {
                                                    navigateTo(
                                                        galleryDestination(
                                                            post = action.post,
                                                            media = action.media,
                                                            startIndex = action.index,
                                                            sharedElementPrefix = commentSharedElementPrefix.withQuotingPostUriPrefix(
                                                                quotingPostUri = action.quotingPostUri,
                                                            ),
                                                        ),
                                                    )
                                                }
                                                is PostAction.OfMore -> {
                                                    postOptionsSheetState.showOptions(action.post)
                                                }
                                                is PostAction.OfMetadata -> Unit
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

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            visible = state.isNotCollapsed,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        ) {
            CommentReplyInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
                    .navigationBarsPadding(),
                paneScaffoldState = paneScaffoldState,
                inputText = inputText,
                onTextChanged = onTextChanged,
                onSendReply = onSendReply,
            )
        }
    }
}

@Composable
private fun CommentReplyInput(
    modifier: Modifier = Modifier,
    paneScaffoldState: PaneScaffoldState,
    inputText: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendReply: () -> Unit,
) {
    var textFieldFocusState by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = ReplyInputShape,
                )
                .padding(vertical = 12.dp)
                .weight(1f)
                .heightIn(max = 80.dp),
        ) {
            var lastFocusState by remember { mutableStateOf(false) }
            BasicTextField(
                value = inputText,
                onValueChange = { onTextChanged(it.withFormattedTextPost()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterStart)
                    .onFocusChanged { state ->
                        if (lastFocusState != state.isFocused) {
                            textFieldFocusState = state.isFocused
                        }
                        lastFocusState = state.isFocused
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default,
                ),
                cursorBrush = SolidColor(LocalContentColor.current),
                textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
            )

            if (inputText.text.isEmpty() && !textFieldFocusState) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                    text = stringResource(Res.string.reply_hint),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        paneScaffoldState.PaneFab(
            modifier = Modifier
                .height(36.dp),
            text = stringResource(Res.string.reply_send),
            icon = null,
            expanded = true,
            enabled = inputText.text.isNotBlank(),
            onClick = onSendReply,
        )
    }
}

val CommentsState.galleryHeightFraction: Float get() = max(
    a = GalleryMinHeightFraction,
    b = 1 - progress,
)

val CommentsState.progress: Float get() = anchoredDraggableState.requireOffset() / height

val CommentsState.isNotCollapsed: Boolean
    get() = anchoredDraggableState.currentValue != Anchor.Collapsed

private val CommentsState.contentOffset
    get() = (height - anchoredDraggableState.requireOffset())
        .toOffset()
        .round()

private fun CommentsState.updateHeight(height: Int) {
    this.height = height.toFloat()
    anchoredDraggableState.updateAnchors(currentDraggableAnchors())
}

private fun CommentsState.currentDraggableAnchors() = DraggableAnchors {
    Anchor.Collapsed at 0f
    Anchor.Halfway at height * ProgressThreshold
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

private val ReplyInputShape = RoundedCornerShape(32.dp)

internal enum class Anchor {
    Collapsed,
    Halfway,
    Expanded,
}

private const val GalleryMinHeightFraction = 0.5f
private const val ProgressThreshold = 0.68f
