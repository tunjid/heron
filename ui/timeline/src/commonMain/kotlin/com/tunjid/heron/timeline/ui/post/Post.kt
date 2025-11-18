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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tunjid.composables.ui.skipIf
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.recordKey
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.avatarSharedElementKey
import com.tunjid.heron.timeline.ui.label.locale
import com.tunjid.heron.timeline.ui.post.threadtraversal.ThreadedVideoPositionState.Companion.childThreadNode
import com.tunjid.heron.timeline.ui.post.threadtraversal.videoId
import com.tunjid.heron.timeline.utilities.createdAt
import com.tunjid.heron.timeline.utilities.format
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.NeutralDialogButton
import com.tunjid.heron.ui.PrimaryDialogButton
import com.tunjid.heron.ui.SimpleDialog
import com.tunjid.heron.ui.SimpleDialogText
import com.tunjid.heron.ui.SimpleDialogTitle
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.shapes.RoundedPolygonShape
import com.tunjid.heron.ui.text.CommonStrings
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import com.tunjid.treenav.compose.moveablesharedelement.updatedMovableStickySharedElementOf
import heron.ui.core.generated.resources.dismiss
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.label_source
import heron.ui.timeline.generated.resources.post_author_label
import heron.ui.timeline.generated.resources.view_labeler
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
fun Post(
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    modifier: Modifier = Modifier,
    now: Instant,
    post: Post,
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
            presentation = presentation,
            appliedLabels = appliedLabels,
            sharedElementPrefix = sharedElementPrefix,
            avatarShape = avatarShape,
            now = now,
            createdAt = createdAt,
            languageTag = Locale.current.toLanguageTag(),
        )
        Column(
            modifier = Modifier
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
                        PostContent.Metadata -> if (isAnchoredInTimeline) MetadataContent(postData)
                        PostContent.Text -> TextContent(postData)
                        PostContent.Labels -> if (postData.hasLabels) LabelContent(postData)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
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
                updatedMovableStickySharedElementOf(
                    modifier = Modifier
                        .size(UiTokens.avatarSize)
                        .clip(data.avatarShape)
                        .clickable {
                            data.postActions.onProfileClicked(
                                profile = data.post.author,
                                post = data.post,
                                quotingPostUri = null,
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
                )
            },
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LabelContent(
    data: PostData,
) = with(data.paneMovableElementSharedTransitionScope) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> FlowRow(
            modifier = Modifier
                .contentPresentationPadding(
                    content = PostContent.Labels,
                    presentation = data.presentation,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            content = {
                data.post.author.labels.forEach { label ->
                    data.withPreferredLabelerAndLocaleInfo(label) { labeler, localeInfo ->
                        val authorLabelContentDescription = stringResource(
                            Res.string.post_author_label,
                            localeInfo.description,
                        )
                        Row(
                            modifier = Modifier
                                .padding(2.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = authorLabelContentDescription
                                }
                                .clip(CircleShape)
                                .clickable {
                                    data.selectedLabel = label
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                    .paneStickySharedElement(
                                        sharedContentState = rememberSharedContentState(
                                            data.sharedElementKey(label),
                                        ),
                                    )
                                    .size(12.dp),
                            )
                            Text(
                                text = localeInfo.name,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                data.selectedLabel?.let { selectedLabel ->
                    data.withPreferredLabelerAndLocaleInfo(selectedLabel) { labeler, localeInfo ->
                        LabelDialog(
                            data = data,
                            localeInfo = localeInfo,
                            labelerHandle = labeler.creator.handle,
                        )
                    }
                }
            },
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
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
                data.postActions.onPostClicked(
                    post = data.post,
                )
            },
            onLinkTargetClicked = { post, linkTarget ->
                data.postActions.onLinkTargetClicked(
                    post = post,
                    linkTarget = linkTarget,
                )
            },
        )

        Timeline.Presentation.Media.Condensed -> Unit
        Timeline.Presentation.Media.Grid -> Unit
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
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
        appliedLabels = data.appliedLabels,
        presentation = data.presentation,
        sharedElementPrefix = data.sharedElementPrefix,
        paneMovableElementSharedTransitionScope = data.paneMovableElementSharedTransitionScope,
        onLinkTargetClicked = data.postActions::onLinkTargetClicked,
        onPostMediaClicked = { media, index, quote ->
            data.postActions.onPostMediaClicked(
                media = media,
                index = index,
                post = quote ?: data.post,
                quotingPostUri = data.post.uri.takeIf { quote != null },
            )
        },
        onEmbeddedRecordClicked = { record ->
            data.postActions.onPostRecordClicked(
                record = record,
                owningPostUri = data.post.uri,
            )
        },
        onQuotedProfileClicked = { quotedPost, quotedProfile ->
            data.postActions.onProfileClicked(
                profile = quotedProfile,
                post = quotedPost,
                quotingPostUri = data.post.uri,
            )
        },
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ActionsContent(
    data: PostData,
) {
    when (data.presentation) {
        Timeline.Presentation.Text.WithEmbed,
        Timeline.Presentation.Media.Expanded,
        -> PostInteractions(
            replyCount = format(data.post.replyCount),
            repostCount = format(data.post.repostCount),
            likeCount = format(data.post.likeCount),
            repostUri = data.post.viewerStats?.repostUri,
            likeUri = data.post.viewerStats?.likeUri,
            isBookmarked = data.post.viewerStats?.bookmarked ?: false,
            postId = data.post.cid,
            postUri = data.post.uri,
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
            onReplyToPost = {
                data.postActions.onReplyToPost(data.post)
            },
            onPostInteraction = data.postActions::onPostInteraction,
            onPostOptionsClicked = {
                data.postActions.onPostOptionsClicked(
                    data.post,
                )
            },
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
        postRecordKey = data.post.uri.recordKey,
        profileId = data.post.author.did,
        reposts = data.post.repostCount,
        quotes = data.post.quoteCount,
        likes = data.post.likeCount,
        onMetadataClicked = data.postActions::onPostMetadataClicked,
    )
}

@Composable
private fun LabelDialog(
    data: PostData,
    labelerHandle: ProfileHandle,
    localeInfo: Labeler.LocaleInfo,
) {
    SimpleDialog(
        onDismissRequest = {
            data.selectedLabel = null
        },
        title = {
            SimpleDialogTitle(text = localeInfo.name)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SimpleDialogText(
                    text = localeInfo.description,
                )
                SimpleDialogText(
                    text = stringResource(
                        Res.string.label_source,
                        labelerHandle.id,
                    ),
                )
            }
        },
        dismissButton = {
            NeutralDialogButton(
                text = stringResource(CommonStrings.dismiss),
                onClick = { data.selectedLabel = null },
            )
        },
        confirmButton = {
            PrimaryDialogButton(
                text = stringResource(Res.string.view_labeler),
                onClick = {
                    data.selectedLabel = null
                    data.postActions.onLinkTargetClicked(
                        post = data.post,
                        linkTarget = LinkTarget.UserHandleMention(labelerHandle),
                    )
                },
            )
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
    presentation: Timeline.Presentation,
    appliedLabels: AppliedLabels,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    createdAt: Instant,
    languageTag: String,
): PostData {
    return remember {
        PostData(
            postActions = postActions,
            paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
            presentationLookaheadScope = presentationLookaheadScope,
            post = post,
            presentation = presentation,
            appliedLabels = appliedLabels,
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
        it.presentation = presentation
        it.appliedLabels = appliedLabels
        it.sharedElementPrefix = sharedElementPrefix
        it.avatarShape = avatarShape
        it.now = now
        it.createdAt = createdAt
        it.languageTag = languageTag
    }
}

@Stable
private class PostData(
    postActions: PostActions,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    presentationLookaheadScope: LookaheadScope,
    post: Post,
    presentation: Timeline.Presentation,
    appliedLabels: AppliedLabels,
    sharedElementPrefix: String,
    avatarShape: RoundedPolygonShape,
    now: Instant,
    created: Instant,
    languageTag: String,
) {
    var postActions by mutableStateOf(postActions)
    var paneMovableElementSharedTransitionScope by mutableStateOf(
        paneMovableElementSharedTransitionScope,
    )
    var presentationLookaheadScope by mutableStateOf(presentationLookaheadScope)
    var post by mutableStateOf(post)
    var presentation by mutableStateOf(presentation)
    var appliedLabels by mutableStateOf(appliedLabels)
    var sharedElementPrefix by mutableStateOf(sharedElementPrefix)
    var avatarShape by mutableStateOf(avatarShape)
    var now by mutableStateOf(now)
    var createdAt by mutableStateOf(created)
    var languageTag by mutableStateOf(languageTag)

    var presentationChanged by mutableStateOf(false)
    var selectedLabel by mutableStateOf<Label?>(null)

    val hasLabels
        get() = post.labels.isNotEmpty() || post.author.labels.isNotEmpty()

    private val labelerDefinitionLookup by derivedStateOf {
        appliedLabels.labelers.associateBy(
            keySelector = { it.creator.did },
            valueTransform = { labeler ->
                labeler to labeler.definitions.associateBy(
                    keySelector = Label.Definition::identifier,
                )
            },
        )
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    val boundsTransform = BoundsTransform { _, _ ->
        SpringSpec.skipIf { !presentationChanged }
    }

    inline fun withPreferredLabelerAndLocaleInfo(
        label: Label,
        labeler: (Labeler, Labeler.LocaleInfo) -> Unit,
    ) {
        val visibility = appliedLabels.visibility(label.value)
        if (visibility != Label.Visibility.Warn) return

        labelerDefinitionLookup[label.creatorId]?.let { (labeler, definitionMap) ->
            definitionMap[label.value]?.let { definition ->
                definition.locale(languageTag)?.let { localeInfo ->
                    labeler(labeler, localeInfo)
                }
            }
        }
    }

    fun sharedElementKey(
        label: Label,
    ) = "$sharedElementPrefix-${post.uri.uri}-${label.creatorId}-${label.value}"
}

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
