package com.tunjid.heron.timeline.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.tunjid.heron.data.core.models.Constants
import com.tunjid.heron.data.core.models.Embed
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.timeline.ui.feature.BlockedPostPost
import com.tunjid.heron.timeline.ui.feature.InvisiblePostPost
import com.tunjid.heron.timeline.ui.feature.PostExternal
import com.tunjid.heron.timeline.ui.feature.PostImages
import com.tunjid.heron.timeline.ui.feature.UnknownPostPost
import com.tunjid.heron.timeline.ui.feature.VisiblePostPost
import com.tunjid.treenav.compose.moveablesharedelement.MovableSharedElementScope
import kotlinx.datetime.Instant

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PostEmbed(
    now: Instant,
    embed: Embed?,
    quote: Post?,
    postId: Id,
    sharedElementPrefix: String,
    animatedVisibilityScope: AnimatedVisibilityScope,
    movableSharedElementScope: MovableSharedElementScope,
    onOpenImage: (Uri) -> Unit,
    onPostClicked: (Post) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column {
        when (embed) {
            is ExternalEmbed -> PostExternal(
                feature = embed,
                postId = postId,
                sharedElementPrefix = sharedElementPrefix,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedTransitionScope = movableSharedElementScope,
                onClick = {
                    uriHandler.openUri(embed.uri.uri)
                },
            )

            is ImageList -> PostImages(embed)
            UnknownEmbed -> UnknownPostPost(onClick = {})
            is Video -> Unit


            null -> Unit
        }
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
                    animatedVisibilityScope = animatedVisibilityScope,
                    sharedTransitionScope = movableSharedElementScope,
                    onClick = {
                        onPostClicked(quote)
                    }
                )
            }
        }
    }
}
