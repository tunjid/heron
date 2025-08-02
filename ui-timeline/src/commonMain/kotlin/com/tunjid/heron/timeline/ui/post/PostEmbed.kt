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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Label
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.PostId
import com.tunjid.heron.timeline.ui.post.feature.BlockedPostPost
import com.tunjid.heron.timeline.ui.post.feature.InvisiblePostPost
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import com.tunjid.heron.timeline.ui.post.feature.UnknownPostPost
import com.tunjid.heron.timeline.ui.withQuotingPostIdPrefix
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope
import heron.ui_timeline.generated.resources.Res
import heron.ui_timeline.generated.resources.sensitive_content
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PostEmbed(
    modifier: Modifier = Modifier,
    now: Instant,
    embed: Embed?,
    quote: Post?,
    postId: PostId,
    sharedElementPrefix: String,
    blurredMediaDefinitions: List<Label.Definition>,
    paneMovableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    onLinkTargetClicked: (Post, LinkTarget) -> Unit,
    onPostMediaClicked: (media: Embed.Media, index: Int, quote: Post?) -> Unit,
    onQuotedPostClicked: (Post) -> Unit,
    onQuotedProfileClicked: (Post, Profile) -> Unit,
    presentation: Timeline.Presentation,
) {
    val uriHandler = LocalUriHandler.current
    var hasClickedThroughBlurredMedia by rememberSaveable(postId) {
        mutableStateOf(false)
    }
    val isBlurred = blurredMediaDefinitions.isNotEmpty() && !hasClickedThroughBlurredMedia

    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (embed) {
                is ExternalEmbed -> PostExternal(
                    feature = embed,
                    postId = postId,
                    sharedElementPrefix = sharedElementPrefix,
                    presentation = presentation,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    onClick = {
                        uriHandler.openUri(embed.uri.uri)
                    },
                )

                is ImageList -> PostImages(
                    feature = embed,
                    postId = postId,
                    sharedElementPrefix = sharedElementPrefix,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    presentation = presentation,
                    isBlurred = isBlurred,
                    onImageClicked = { index ->
                        onPostMediaClicked(embed, index, null)
                    }
                )

                UnknownEmbed -> UnknownPostPost(onClick = {})
                is Video -> PostVideo(
                    video = embed,
                    postId = postId,
                    paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                    sharedElementPrefix = sharedElementPrefix,
                    isBlurred = isBlurred,
                    presentation = presentation,
                    onClicked = {
                        onPostMediaClicked(embed, 0, null)
                    }
                )

                null -> Unit
            }
            if (presentation == Timeline.Presentation.Text.WithEmbed) {
                if (quote != null) Spacer(Modifier.height(16.dp))
                when (quote?.cid) {
                    null -> Unit
                    Constants.notFoundPostId -> InvisiblePostPost(onClick = {})
                    Constants.blockedPostId -> BlockedPostPost(onClick = {})
                    Constants.unknownPostId -> UnknownPostPost(onClick = {})
                    else -> QuotedPost(
                        now = now,
                        quotedPost = quote,
                        sharedElementPrefix = sharedElementPrefix.withQuotingPostIdPrefix(
                            quotingPostId = postId,
                        ),
                        isBlurred = blurredMediaDefinitions.isNotEmpty(),
                        paneMovableElementSharedTransitionScope = paneMovableElementSharedTransitionScope,
                        onLinkTargetClicked = onLinkTargetClicked,
                        onProfileClicked = onQuotedProfileClicked,
                        onPostMediaClicked = onPostMediaClicked,
                        onClick = {
                            onQuotedPostClicked(quote)
                        }
                    )
                }
            }
        }

        val canClickThrough = blurredMediaDefinitions.none {
            it.severity == Label.Severity.None
        }
        if (isBlurred && canClickThrough && !hasClickedThroughBlurredMedia) {
            SensitiveContentButton(
                modifier = Modifier
                    .align(Alignment.Center),
                blurredMediaDefinitions = blurredMediaDefinitions,
                onClick = {
                    hasClickedThroughBlurredMedia = true
                },
            )
        }
    }
}

@Composable
private fun SensitiveContentButton(
    modifier: Modifier,
    blurredMediaDefinitions: List<Label.Definition>,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier,
        onClick = {
            onClick()
        },
        content = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (blurredMediaDefinitions.firstOrNull()?.severity) {
                    Label.Severity.Alert -> Icon(
                        imageVector = Icons.Rounded.Report,
                        contentDescription = "",
                    )

                    Label.Severity.Inform -> Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "",
                    )

                    Label.Severity.None,
                    null -> Unit
                }
                Text(
                    text = stringResource(Res.string.sensitive_content)
                )
            }
        },
    )
}

