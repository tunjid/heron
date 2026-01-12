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

package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.composables.ui.skipIf
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.ThreadGate
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.allowsAll
import com.tunjid.heron.data.core.models.allowsNone
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.PostAction
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.ui.post.threadtraversal.videoId
import com.tunjid.heron.timeline.utilities.AppliedLabelDialog
import com.tunjid.heron.timeline.utilities.Label
import com.tunjid.heron.timeline.utilities.LabelFlowRow
import com.tunjid.heron.timeline.utilities.LabelIconSize
import com.tunjid.heron.timeline.utilities.LabelText
import com.tunjid.heron.timeline.utilities.SensitiveContentBox
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.timeline.utilities.forEach
import com.tunjid.heron.timeline.utilities.icon
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.modifiers.ifTrue
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.UpdatedMovableStickySharedElementOf
import heron.ui.core.generated.resources.post_author_label
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.mute_words_post_hidden
import heron.ui.timeline.generated.resources.sensitive_media
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Post(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    modifier: Modifier = Modifier,
    now: Instant,
    post: Post,
    hasMutedWords: Boolean,
    threadGate: ThreadGate?,
    isAnchoredInTimeline: Boolean,
    avatarShape: RoundedPolygonShape,
    sharedElementPrefix: String,
    createdAt: Instant,
    presentation: Timeline.Presentation,
    appliedLabels: AppliedLabels,
    postActions: PostActions,
    timeline: @Composable (BoxScope.() -> Unit) = {},
) {
    Box(
        modifier = modifier
            .childThreadNode(videoId = post.videoId),
    ) {
        if (presentation == Timeline.Presentation.Text.WithEmbed) Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp),
            content = timeline,
        )
        val postData = rememberUpdatedPostData(
            postActions = postActions,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            presentationLookaheadScope = presentationLookaheadScope,
            post = post,
            threadGate = threadGate,
            presentation = presentation,
            appliedLabels = appliedLabels,
            hasMutedWords = hasMutedWords,
            sharedElementPrefix = sharedElementPrefix,
            avatarShape = avatarShape,
            now = now,
            createdAt = createdAt,
            languageTag = Locale.current.toLanguageTag(),
        )
        SensitiveContentBox(
            modifier = Modifier
                .fillMaxWidth(),
            isBlurred = postData.textBlurred,
            canUnblur = true,
            label = stringResource(Res.string.mute_words_post_hidden),
            icon = Icons.Rounded.VisibilityOff,
            onUnblurClicked = {
                postData.hasClickedThroughMutedWords = true
            },
        ) {
            Column(
                modifier = Modifier
                    .ifTrue(postData.textBlurred) {
                        sensitiveContentBlur(MutedWordShape)
                    }
                    .padding(vertical = presentation.postVerticalPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(presentation.postContentSpacing),
            ) {
                presentation.contentOrder.forEach { order ->
                    key(order.key) {
                        when (order) {
                            PostContent.Actions -> ActionsContent(postData)
                            PostContent.Attribution -> AttributionContent(postData)
                            PostContent.Embed.Link -> EmbedContent(postData)
                            PostContent.Embed.Media -> EmbedContent(postData)
                            PostContent.Metadata -> if (isAnchoredInTimeline) MetadataContent(
                                postData,
                            )
                            PostContent.Text -> TextContent(postData)
                            PostContent.Labels -> if (postData.hasLabels) LabelContent(postData)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributionContent(
    data: PostData,
) = with(data.paneMovableElementSharedTransitionScope) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> AttributionLayout(
            modifier = Modifier
                .contentPresentationPadding(
                    content = PostContent.Attribution,
                    presentation = data.presentation,
                ),
            avatar = {
                UpdatedMovableStickySharedElementOf(
                    modifier = Modifier
                        .size(UiTokens.avatarSize)
                        .clip(data.avatarShape)
                        .clickable {
                            data.postActions.onPostAction(
                                PostAction.OfProfile(
                                    profile = data.post.author,
                                    post = data.post,
                                    quotingPostUri = null,
                                ),
                            )
                        },
                    sharedContentState = rememberSharedContentState(
                        key = data.post.avatarSharedElementKey(data.sharedElementPrefix),
                    ),
                    state = remember(data.post.author.avatar) {
                        ImageArgs(
                            url = data.post.author.avatar?.uri,
                            contentScale = ContentScale.Crop,
                            contentDescription = data.post.author.displayName
                                ?: data.post.author.handle.id,
                            shape = data.avatarShape,
                        )
                    },
                    sharedElement = { state, modifier ->
                        AsyncImage(state, modifier)
                    },
                )
            },
            label = {
                PostHeadline(
                    now = data.now,
                    createdAt = data.createdAt,
                    author = data.post.author,
                    postId = data.post.cid,
                    sharedElementPrefix = data.sharedElementPrefix,
                    paneMovableElementSharedTransitionScope = this,
                    onPostClicked = {
                        data.postActions.onPostAction(
                            PostAction.OfPost(
                                post = data.post,
                            ),
                        )
                    },
                    onAuthorClicked = {
                        data.postActions.onPostAction(
                            PostAction.OfProfile(
                                profile = data.post.author,
                                post = data.post,
                                quotingPostUri = null,
                            ),
                        )
                    },
                )
            },
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@Composable
private fun LabelContent(
    data: PostData,
) = with(data.paneMovableElementSharedTransitionScope) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> LabelFlowRow(
            modifier = Modifier
                .contentPresentationPadding(
                    content = PostContent.Labels,
                    presentation = data.presentation,
                ),
            content = {
                data.appliedLabels.forEach(
                    languageTag = data.languageTag,
                    labels = data.post.author.labels,
                ) { label, labeler, localeInfo ->
                    val authorLabelContentDescription = stringResource(
                        CommonStrings.post_author_label,
                        localeInfo.description,
                    )
                    Label(
                        modifier = Modifier
                            .padding(2.dp),
                        contentDescription = authorLabelContentDescription,
                        icon = {
                            PaneStickySharedElement(
                                modifier = Modifier
                                    .size(LabelIconSize),
                                sharedContentState = rememberSharedContentState(
                                    data.sharedElementKey(label),
                                ),
                            ) {
                                AsyncImage(
                                    args = remember(labeler.creator.avatar) {
                                        ImageArgs(
                                            url = labeler.creator.avatar?.uri,
                                            contentScale = ContentScale.Crop,
                                            contentDescription = null,
                                            shape = data.avatarShape,
                                        )
                                    },
                                    modifier = Modifier
                                        .fillParentAxisIfFixedOrWrap(),
                                )
                            }
                        },
                        description = {
                            LabelText(localeInfo.name)
                        },
                        onClick = {
                            data.selectedLabel = label
                        },
                    )
                }
                data.selectedLabel?.let { selectedLabel ->
                    AppliedLabelDialog(
                        label = selectedLabel,
                        languageTag = data.languageTag,
                        appliedLabels = data.appliedLabels,
                        onDismiss = {
                            data.selectedLabel = null
                        },
                        onLabelerClicked = { labeler ->
                            data.selectedLabel = null
                            data.postActions.onPostAction(
                                PostAction.OfLinkTarget(
                                    post = data.post,
                                    linkTarget = LinkTarget.UserHandleMention(
                                        labeler.creator.handle,
                                    ),
                                ),
                            )
                        },
                    )
                }
            },
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@Composable
private fun TextContent(
    data: PostData,
) = with(data.paneMovableElementSharedTransitionScope) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> PostText(
            post = data.post,
            sharedElementPrefix = data.sharedElementPrefix,
            paneMovableElementSharedTransitionScope = this,
            modifier = Modifier
                .zIndex(TextContentZIndex)
                .contentPresentationPadding(
                    content = PostContent.Text,
                    presentation = data.presentation,
                )
                .animateBounds(
                    lookaheadScope = data.presentationLookaheadScope,
                    boundsTransform = data.boundsTransform,
                )
                .fillMaxWidth(),
            maxLines = when (data.presentation) {
                Timeline.Presentation.Text.WithEmbed -> Int.MAX_VALUE
                Timeline.Presentation.Media.Condensed -> throw IllegalArgumentException(
                    "Condensed media should not show text",
                )
                Timeline.Presentation.Media.Grid -> throw IllegalArgumentException(
                    "Grid media should not show text",
                )
                Timeline.Presentation.Media.Expanded -> 2
            },
            onClick = {
                data.postActions.onPostAction(
                    PostAction.OfPost(
                        post = data.post,
                    ),
                )
            },
            onLinkTargetClicked = { post, linkTarget ->
                data.postActions.onPostAction(
                    PostAction.OfLinkTarget(
                        post = post,
                        linkTarget = linkTarget,
                    ),
                )
            },
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@Composable
private fun EmbedContent(
    data: PostData,
) {
    PostEmbed(
        modifier = Modifier
            .zIndex(EmbedContentZIndex)
            .contentPresentationPadding(
                content = data.post.embed.asPostContent(),
                presentation = data.presentation,
            )
            .animateBounds(
                lookaheadScope = data.presentationLookaheadScope,
                boundsTransform = data.boundsTransform,
            )
            .fillMaxWidth(),
        now = data.now,
        embed = data.post.embed,
        embeddedRecord = data.post.embeddedRecord,
        postUri = data.post.uri,
        isBlurred = data.mediaBlurred,
        canUnblur = data.canUnblurMedia,
        blurIcon = data.appliedLabels.blurredMediaSeverity.icon(),
        blurLabel = stringResource(Res.string.sensitive_media),
        appliedLabels = data.appliedLabels,
        presentation = data.presentation,
        sharedElementPrefix = data.sharedElementPrefix,
        paneMovableElementSharedTransitionScope = data.paneMovableElementSharedTransitionScope,
        onUnblurClicked = {
            data.hasClickedThroughSensitiveMedia = true
        },
        onLinkTargetClicked = { post, linkTarget ->
            data.postActions.onPostAction(
                PostAction.OfLinkTarget(
                    post = post,
                    linkTarget = linkTarget,
                ),
            )
        },
        onPostMediaClicked = { media, index, quote ->
            data.postActions.onPostAction(
                PostAction.OfMedia(
                    media = media,
                    index = index,
                    post = quote ?: data.post,
                    quotingPostUri = data.post.uri.takeIf { quote != null },
                ),
            )
        },
        onEmbeddedRecordClicked = { record ->
            data.postActions.onPostAction(
                PostAction.OfRecord(
                    record = record,
                    owningPostUri = data.post.uri,
                ),
            )
        },
        onQuotedProfileClicked = { quotedPost, quotedProfile ->
            data.postActions.onPostAction(
                PostAction.OfProfile(
                    profile = quotedProfile,
                    post = quotedPost,
                    quotingPostUri = data.post.uri,
                ),
            )
        },
    )
}

@Composable
private fun ActionsContent(
    data: PostData,
) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> PostInteractions(
            post = data.post,
            sharedElementPrefix = data.sharedElementPrefix,
            presentation = data.presentation,
            paneMovableElementSharedTransitionScope = data.paneMovableElementSharedTransitionScope,
            modifier = Modifier
                .contentPresentationPadding(
                    content = PostContent.Actions,
                    presentation = data.presentation,
                )
                .animateBounds(
                    lookaheadScope = data.presentationLookaheadScope,
                    boundsTransform = data.boundsTransform,
                ),
            onInteraction = data.postActions::onPostAction,
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@Composable
private fun MetadataContent(
    data: PostData,
) {
    PostMetadata(
        modifier = Modifier
            .padding(
                horizontal = 24.dp,
                vertical = 4.dp,
            ),
        time = data.post.createdAt,
        replyStatus = data.replyStatus,
        postUri = data.post.uri,
        profileId = data.post.author.did,
        reposts = data.post.repostCount,
        quotes = data.post.quoteCount,
        likes = data.post.likeCount,
        onMetadataClicked = {
            data.postActions.onPostAction(PostAction.OfMetadata(it))
        },
    )
}

private fun Modifier.contentPresentationPadding(
    content: PostContent,
    presentation: Timeline.Presentation,
) = padding(
    start = presentation.postContentStartPadding(content),
    end = presentation.postContentEndPadding(content),
)

/**
 * Vertical content padding for the post composable
 */
private val Timeline.Presentation.postVerticalPadding: Dp
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

private val Timeline.Presentation.postContentSpacing: Dp
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

private fun Timeline.Presentation.postContentStartPadding(
    content: PostContent,
) = when (content) {
    PostContent.Actions -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 24.dp
        Timeline.Presentation.Media.Expanded -> 16.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    PostContent.Attribution -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    is PostContent.Embed -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 24.dp
        Timeline.Presentation.Media.Expanded -> when (content) {
            PostContent.Embed.Link -> 8.dp
            PostContent.Embed.Media -> 0.dp
        }

        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    PostContent.Text -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 24.dp
        Timeline.Presentation.Media.Expanded -> 16.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    PostContent.Metadata -> 0.dp

    PostContent.Labels -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 24.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }
}

private fun Timeline.Presentation.postContentEndPadding(
    content: PostContent,
) = when (content) {
    PostContent.Actions -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 16.dp
        Timeline.Presentation.Media.Expanded -> 16.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    PostContent.Attribution -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 8.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    is PostContent.Embed -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 16.dp
        Timeline.Presentation.Media.Expanded -> when (content) {
            PostContent.Embed.Link -> 8.dp
            PostContent.Embed.Media -> 0.dp
        }

        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    PostContent.Text -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 16.dp
        Timeline.Presentation.Media.Expanded -> 16.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }

    PostContent.Metadata -> 0.dp

    PostContent.Labels -> when (this) {
        Timeline.Presentation.Text.WithEmbed -> 24.dp
        Timeline.Presentation.Media.Expanded -> 8.dp
        Timeline.Presentation.Media.Condensed -> 0.dp
        Timeline.Presentation.Media.Grid -> 0.dp
    }
}

private fun Embed?.asPostContent() = when (this) {
    is ImageList,
    is Video,
    -> PostContent.Embed.Media

    null,
    UnknownEmbed,
    is ExternalEmbed,
    -> PostContent.Embed.Link
}

@Composable
private fun rememberUpdatedPostData(
    postActions: PostActions,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    post: Post,
    threadGate: ThreadGate?,
    presentation: Timeline.Presentation,
    appliedLabels: AppliedLabels,
    hasMutedWords: Boolean,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    createdAt: Instant,
    languageTag: String,
): PostData = rememberSaveable(
    saver = listSaver(
        save = { data ->
            listOf(
                data.hasClickedThroughMutedWords,
                data.hasClickedThroughSensitiveMedia,
            )
        },
        restore = { (hasClickedThroughMutedWords, hasClickedThroughSensitiveMedia) ->
            PostData(
                postActions = postActions,
                paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                presentationLookaheadScope = presentationLookaheadScope,
                post = post,
                threadGate = threadGate,
                presentation = presentation,
                appliedLabels = appliedLabels,
                hasMutedWords = hasMutedWords,
                sharedElementPrefix = sharedElementPrefix,
                avatarShape = avatarShape,
                now = now,
                created = createdAt,
                languageTag = languageTag,
                hasClickedThroughMutedWords = hasClickedThroughMutedWords,
                hasClickedThroughSensitiveMedia = hasClickedThroughSensitiveMedia,
            )
        },
    ),
) {
    PostData(
        postActions = postActions,
        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
        presentationLookaheadScope = presentationLookaheadScope,
        post = post,
        threadGate = threadGate,
        presentation = presentation,
        appliedLabels = appliedLabels,
        hasMutedWords = hasMutedWords,
        sharedElementPrefix = sharedElementPrefix,
        avatarShape = avatarShape,
        now = now,
        created = createdAt,
        languageTag = languageTag,
    )
}.also {
    if (it.presentation != presentation) it.presentationChanged = true
    it.postActions = postActions
    it.paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope
    it.presentationLookaheadScope = presentationLookaheadScope
    it.post = post
    it.threadGate = threadGate
    it.presentation = presentation
    it.appliedLabels = appliedLabels
    it.hasMutedWords = hasMutedWords
    it.sharedElementPrefix = sharedElementPrefix
    it.avatarShape = avatarShape
    it.now = now
    it.createdAt = createdAt
    it.languageTag = languageTag
}

@Stable
private class PostData(
    postActions: PostActions,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    post: Post,
    threadGate: ThreadGate?,
    presentation: Timeline.Presentation,
    appliedLabels: AppliedLabels,
    hasMutedWords: Boolean,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    created: Instant,
    languageTag: String,
    hasClickedThroughMutedWords: Boolean = false,
    hasClickedThroughSensitiveMedia: Boolean = false,
) {
    var postActions by mutableStateOf(postActions)
    var paneMovableElementSharedTransitionScope by mutableStateOf(
        paneMovableElementSharedTransitionScope,
    )
    var presentationLookaheadScope by mutableStateOf(presentationLookaheadScope)
    var post by mutableStateOf(post)
    var threadGate by mutableStateOf(threadGate)
    var presentation by mutableStateOf(presentation)
    var appliedLabels by mutableStateOf(appliedLabels)
    var hasMutedWords by mutableStateOf(hasMutedWords)
    var sharedElementPrefix by mutableStateOf(sharedElementPrefix)
    var avatarShape by mutableStateOf(avatarShape)
    var now by mutableStateOf(now)
    var createdAt by mutableStateOf(created)
    var languageTag by mutableStateOf(languageTag)

    var presentationChanged by mutableStateOf(false)
    var selectedLabel by mutableStateOf<Label?>(null)

    var hasClickedThroughMutedWords by mutableStateOf(hasClickedThroughMutedWords)
    var hasClickedThroughSensitiveMedia by mutableStateOf(hasClickedThroughSensitiveMedia)

    val hasLabels
        get() = post.labels.isNotEmpty() || post.author.labels.isNotEmpty()

    val replyStatus
        get() = when (val gate = threadGate) {
            null -> PostReplyStatus.All
            else -> when {
                gate.allowed.allowsAll -> PostReplyStatus.All
                gate.allowed.allowsNone -> PostReplyStatus.None
                else -> PostReplyStatus.Some
            }
        }

    val boundsTransform = BoundsTransform { _, _ ->
        SpringSpec.skipIf { !presentationChanged }
    }

    fun sharedElementKey(
        label: Label,
    ) = "$sharedElementPrefix-${post.uri.uri}-${label.creatorId}-${label.value}"
}

private val PostData.textBlurred: Boolean
    get() = hasMutedWords && !hasClickedThroughMutedWords

private val PostData.mediaBlurred: Boolean
    get() = appliedLabels.shouldBlurMedia && !hasClickedThroughSensitiveMedia

private val PostData.canUnblurMedia: Boolean
    get() = appliedLabels.blurredMediaSeverity != Label.Severity.None

private sealed class PostContent(val key: String) {
    data object Attribution : PostContent(key = "Attribution")
    data object Text : PostContent(key = "Text")
    sealed class Embed : PostContent(key = "Embed") {
        data object Link : Embed()
        data object Media : Embed()
    }

    data object Metadata : PostContent(key = "Metadata")
    data object Actions : PostContent(key = "Actions")
    data object Labels : PostContent(key = "Labels")
}

@Stable
private val Timeline.Presentation.contentOrder
    get() = when (this) {
        Timeline.Presentation.Text.WithEmbed -> TextAndEmbedOrder
        Timeline.Presentation.Media.Expanded -> ExpandedMediaOrder
        Timeline.Presentation.Media.Condensed -> CondensedMediaOrder
        Timeline.Presentation.Media.Grid -> GridMediaOrder
    }

private val SpringSpec = spring<Rect>(
    stiffness = Spring.StiffnessMediumLow,
)

private val TextAndEmbedOrder = listOf(
    PostContent.Attribution,
    PostContent.Labels,
    PostContent.Text,
    PostContent.Embed.Media,
    PostContent.Metadata,
    PostContent.Actions,
)

private val ExpandedMediaOrder = listOf(
    PostContent.Attribution,
    PostContent.Labels,
    PostContent.Embed.Media,
    PostContent.Actions,
    PostContent.Text,
    PostContent.Metadata,
)

private val CondensedMediaOrder = listOf(
    PostContent.Attribution,
    PostContent.Labels,
    PostContent.Text,
    PostContent.Embed.Media,
    PostContent.Metadata,
    PostContent.Actions,
)

private val GridMediaOrder = listOf(
    PostContent.Attribution,
    PostContent.Labels,
    PostContent.Text,
    PostContent.Embed.Media,
    PostContent.Metadata,
    PostContent.Actions,
)

private const val EmbedContentZIndex = 2f
private const val TextContentZIndex = 1f

private val MutedWordShape = RoundedCornerShape(8.dp)
