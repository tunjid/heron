package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.aspectRatio
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.media.video.LocalVideoPlayerController
import com.tunjid.heron.media.video.VideoPlayer
import com.tunjid.heron.media.video.rememberUpdatedVideoPlayerState
import com.tunjid.heron.timeline.ui.post.feature.BlockedPostPost
import com.tunjid.heron.timeline.ui.post.feature.InvisiblePostPost
import com.tunjid.heron.timeline.ui.post.feature.UnknownPostPost
import com.tunjid.heron.timeline.ui.post.feature.VisiblePostPost
import com.tunjid.heron.ui.SharedElementScope
import kotlinx.datetime.Instant

@Composable
internal fun PostEmbed(
    now: Instant,
    embed: Embed?,
    quote: Post?,
    postId: Id,
    sharedElementPrefix: String,
    sharedElementScope: SharedElementScope,
    onPostMediaClicked: (Embed.Media, Int) -> Unit,
    onPostClicked: (Post) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column {
        when (embed) {
            is ExternalEmbed -> PostExternal(
                feature = embed,
                postId = postId,
                sharedElementPrefix = sharedElementPrefix,
                sharedElementScope = sharedElementScope,
                onClick = {
                    uriHandler.openUri(embed.uri.uri)
                },
            )

            is ImageList -> PostImages(
                feature = embed,
                sharedElementPrefix = sharedElementPrefix,
                sharedElementScope = sharedElementScope,
                onImageClicked = { index ->
                    onPostMediaClicked(embed, index)
                }
            )

            UnknownEmbed -> UnknownPostPost(onClick = {})
            is Video -> VideoPlayer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (!embed.aspectRatio.isNaN()) embed.aspectRatio else 1f),
                state = LocalVideoPlayerController.current.rememberUpdatedVideoPlayerState(
                    videoUrl = embed.playlist.uri,
                )
            )

            null -> Unit
        }
        if (quote != null) Spacer(Modifier.height(16.dp))
        when (quote?.cid) {
            null -> Unit
            Constants.notFoundPostId -> InvisiblePostPost(onClick = {})
            Constants.blockedPostId -> BlockedPostPost(onClick = {})
            Constants.unknownPostId -> UnknownPostPost(onClick = {})
            else -> {
                VisiblePostPost(
                    now = now,
                    post = quote,
                    author = quote.author,
                    sharedElementPrefix = sharedElementPrefix,
                    sharedTransitionScope = sharedElementScope,
                    onClick = {
                        onPostClicked(quote)
                    }
                )
            }
        }
    }
}
