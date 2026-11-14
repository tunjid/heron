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

package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.externalEmbeddedRecord
import com.tunjid.heron.data.core.models.nativeEmbeddedRecord
import com.tunjid.heron.timeline.ui.post.PostEmbed
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.modifiers.ifTrue
import kotlin.time.Clock

@Composable
fun NotificationEmbed(
    modifier: Modifier = Modifier,
    postNotification: Notification.PostAssociated,
    paneTransitionScope: PaneTransitionScope,
    onPostMediaClicked: (Notification.PostAssociated, Embed.Media, Int, Post?) -> Unit,
    onLinkTargetClicked: (Notification.PostAssociated, LinkTarget) -> Unit,
    onEmbeddedRecordClicked: (Notification.PostAssociated, Record) -> Unit,
    onQuotedProfileClicked: (Notification.PostAssociated, Post, Profile) -> Unit,
) {
    val post = postNotification.associatedPost
    if (post.nativeEmbeddedRecord == null && post.embed == null) return
    PostEmbed(
        modifier = modifier
            .ifTrue(
                predicate = post.nativeEmbeddedRecord !is Post,
                block = Modifier::notificationMediaSizeAndClip,
            ),
        now = remember { Clock.System.now() },
        embed = post.embed,
        nativeEmbeddedRecord = post.nativeEmbeddedRecord,
        externalEmbeddedRecord = post.externalEmbeddedRecord,
        postUri = post.uri,
        isBlurred = false,
        canUnblur = true,
        blurLabel = "",
        blurIcon = null,
        sharedElementPrefix = postNotification.sharedElementPrefix(),
        appliedLabels = AppliedLabels.Empty,
        paneTransitionScope = paneTransitionScope,
        onUnblurClicked = { },
        onLinkTargetClicked = { _, linkTarget ->
            onLinkTargetClicked(
                postNotification,
                linkTarget,
            )
        },
        onPostMediaClicked = { media, index, quote ->
            onPostMediaClicked(
                postNotification,
                media,
                index,
                quote,
            )
        },
        onEmbeddedRecordClicked = { record ->
            onEmbeddedRecordClicked(
                postNotification,
                record,
            )
        },
        onQuotedProfileClicked = { quotedPost, profile ->
            onQuotedProfileClicked(
                postNotification,
                quotedPost,
                profile,
            )
        },
        onRecordInteraction = {},
        presentation = when (post.nativeEmbeddedRecord) {
            is Post -> Timeline.Presentation.Text.WithEmbed
            else -> Timeline.Presentation.Media.Condensed
        },
    )
}

private fun Modifier.notificationMediaSizeAndClip() =
    width(104.dp)
        .clip(RoundedCornerShape(4.dp))
