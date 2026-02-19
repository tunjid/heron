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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.label.Labeler
import com.tunjid.heron.timeline.ui.list.FeedList
import com.tunjid.heron.timeline.ui.list.StarterPack
import com.tunjid.heron.timeline.ui.post.feature.BlockedPostPost
import com.tunjid.heron.timeline.ui.post.feature.FeatureContainer
import com.tunjid.heron.timeline.ui.post.feature.InvisiblePostPost
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import com.tunjid.heron.timeline.ui.post.feature.UnknownPostPost
import com.tunjid.heron.timeline.ui.withQuotingPostUriPrefix
import com.tunjid.heron.timeline.utilities.SensitiveContentBox
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import kotlin.time.Instant

@Composable
internal fun PostEmbed(
    modifier: Modifier = Modifier,
    now: Instant,
    embed: Embed?,
    embeddedRecord: Record.Embeddable?,
    postUri: PostUri,
    isBlurred: Boolean,
    canUnblur: Boolean,
    blurLabel: String,
    blurIcon: ImageVector?,
    sharedElementPrefix: String,
    appliedLabels: AppliedLabels,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onUnblurClicked: () -> Unit,
    onLinkTargetClicked: (Post, LinkTarget) -> Unit,
    onPostMediaClicked: (media: Embed.Media, index: Int, quote: Post?) -> Unit,
    onEmbeddedRecordClicked: (Record) -> Unit,
    onQuotedProfileClicked: (Post, Profile) -> Unit,
    presentation: Timeline.Presentation,
) {
    val uriHandler = LocalUriHandler.current
    SensitiveContentBox(
        modifier = modifier,
        isBlurred = isBlurred,
        canUnblur = canUnblur,
        label = blurLabel,
        icon = blurIcon,
        onUnblurClicked = onUnblurClicked,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (embed) {
                is ExternalEmbed ->
                    PostExternal(
                        feature = embed,
                        postUri = postUri,
                        sharedElementPrefix = sharedElementPrefix,
                        presentation = presentation,
                        isBlurred = isBlurred,
                        paneMovableElementSharedTransitionScope =
                            paneMovableElementSharedTransitionScope,
                        onClick = { uriHandler.openUri(embed.uri.uri) },
                    )

                is ImageList ->
                    PostImages(
                        modifier = Modifier.fillMaxWidth(),
                        feature = embed,
                        postUri = postUri,
                        sharedElementPrefix = sharedElementPrefix,
                        paneMovableElementSharedTransitionScope =
                            paneMovableElementSharedTransitionScope,
                        presentation = presentation,
                        isBlurred = isBlurred,
                        matchHeightConstraintsFirst = false,
                        onImageClicked = { index -> onPostMediaClicked(embed, index, null) },
                    )

                UnknownEmbed -> UnknownPostPost(onClick = {})
                is Video ->
                    PostVideo(
                        modifier = Modifier.fillMaxWidth(),
                        video = embed,
                        postUri = postUri,
                        paneMovableElementSharedTransitionScope =
                            paneMovableElementSharedTransitionScope,
                        sharedElementPrefix = sharedElementPrefix,
                        isBlurred = isBlurred,
                        matchHeightConstraintsFirst = false,
                        presentation = presentation,
                        onClicked = { onPostMediaClicked(embed, 0, null) },
                    )

                null -> Unit
            }
            if (presentation == Timeline.Presentation.Text.WithEmbed) {
                if (embeddedRecord != null) Spacer(Modifier.height(16.dp))
                when (embeddedRecord) {
                    is Post ->
                        when (embeddedRecord.cid) {
                            Constants.notFoundPostId -> InvisiblePostPost(onClick = null)
                            Constants.blockedPostId -> BlockedPostPost(onClick = null)
                            Constants.unknownPostId -> UnknownPostPost(onClick = null)
                            else ->
                                QuotedPost(
                                    now = now,
                                    quotedPost = embeddedRecord,
                                    sharedElementPrefix =
                                        sharedElementPrefix.withQuotingPostUriPrefix(
                                            quotingPostUri = postUri
                                        ),
                                    isBlurred = appliedLabels.shouldBlurMedia,
                                    paneMovableElementSharedTransitionScope =
                                        paneMovableElementSharedTransitionScope,
                                    onLinkTargetClicked = onLinkTargetClicked,
                                    onProfileClicked = onQuotedProfileClicked,
                                    onPostMediaClicked = onPostMediaClicked,
                                    onClick = { onEmbeddedRecordClicked(embeddedRecord) },
                                )
                        }
                    is FeedGenerator ->
                        FeatureContainer(onClick = { onEmbeddedRecordClicked(embeddedRecord) }) {
                            FeedGenerator(
                                modifier = Modifier.padding(12.dp),
                                movableElementSharedTransitionScope =
                                    paneMovableElementSharedTransitionScope,
                                sharedElementPrefix =
                                    sharedElementPrefix.withQuotingPostUriPrefix(
                                        quotingPostUri = postUri
                                    ),
                                feedGenerator = embeddedRecord,
                                status = null,
                                onFeedGeneratorStatusUpdated = {},
                            )
                        }
                    is FeedList ->
                        FeatureContainer(onClick = { onEmbeddedRecordClicked(embeddedRecord) }) {
                            FeedList(
                                modifier = Modifier.padding(12.dp),
                                movableElementSharedTransitionScope =
                                    paneMovableElementSharedTransitionScope,
                                sharedElementPrefix =
                                    sharedElementPrefix.withQuotingPostUriPrefix(
                                        quotingPostUri = postUri
                                    ),
                                list = embeddedRecord,
                                status = null,
                                onListStatusUpdated = {},
                            )
                        }
                    is StarterPack ->
                        FeatureContainer(onClick = { onEmbeddedRecordClicked(embeddedRecord) }) {
                            StarterPack(
                                modifier = Modifier.padding(12.dp),
                                movableElementSharedTransitionScope =
                                    paneMovableElementSharedTransitionScope,
                                sharedElementPrefix =
                                    sharedElementPrefix.withQuotingPostUriPrefix(
                                        quotingPostUri = postUri
                                    ),
                                starterPack = embeddedRecord,
                            )
                        }
                    is Labeler ->
                        Labeler(
                            modifier = Modifier.padding(12.dp),
                            movableElementSharedTransitionScope =
                                paneMovableElementSharedTransitionScope,
                            sharedElementPrefix =
                                sharedElementPrefix.withQuotingPostUriPrefix(
                                    quotingPostUri = postUri
                                ),
                            labeler = embeddedRecord,
                        )
                    null -> Unit
                }
            }
        }
    }
}
