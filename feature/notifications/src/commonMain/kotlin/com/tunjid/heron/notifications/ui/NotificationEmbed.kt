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
    onPostMediaClicked: (Notification.PostAssociated, Embed.Media, Int) -> Unit,
    onLinkTargetClicked: (Notification.PostAssociated, LinkTarget) -> Unit,
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
        onPostMediaClicked = { media, index, _ ->
            onPostMediaClicked(
                postNotification,
                media,
                index,
            )
        },
        onEmbeddedRecordClicked = {},
        onQuotedProfileClicked = { _, _ -> },
        onPublicationSubscriptionToggled = { },
        presentation = when (post.nativeEmbeddedRecord) {
            is Post -> Timeline.Presentation.Text.WithEmbed
            else -> Timeline.Presentation.Media.Condensed
        },
    )
}

private fun Modifier.notificationMediaSizeAndClip() =
    width(104.dp)
        .clip(RoundedCornerShape(4.dp))
