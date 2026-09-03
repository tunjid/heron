package com.tunjid.heron.notifications.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.models.Notification
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.externalEmbeddedRecord
import com.tunjid.heron.data.core.models.nativeEmbeddedRecord
import com.tunjid.heron.timeline.ui.post.PostEmbed
import com.tunjid.heron.ui.PaneTransitionScope
import kotlin.time.Clock

@Composable
fun NotificationEmbed(
    modifier: Modifier = Modifier,
    postNotification: Notification.PostAssociated,
    paneTransitionScope: PaneTransitionScope,
    onPostMediaClicked: (Notification.PostAssociated, Embed.Media, Int) -> Unit = { _, _, _ -> },
    onLinkTargetClicked: (Notification.PostAssociated, LinkTarget) -> Unit = { _, _ -> },
) {
    PostEmbed(
        modifier = modifier
            .width(104.dp),
        now = remember { Clock.System.now() },
        embed = postNotification.associatedPost.embed,
        nativeEmbeddedRecord = postNotification.associatedPost.nativeEmbeddedRecord,
        externalEmbeddedRecord = postNotification.associatedPost.externalEmbeddedRecord,
        postUri = postNotification.associatedPost.uri,
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
        presentation = Timeline.Presentation.Text.WithEmbed,
    )
}
