package com.tunjid.heron.timeline.ui.post

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.tunjid.heron.data.core.types.Id
import com.tunjid.heron.data.core.types.Uri
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
                sharedElementScope = sharedElementScope,
                onClick = {
                    uriHandler.openUri(embed.uri.uri)
                },
            )

            is ImageList -> PostImages(
                feature = embed,
                sharedElementPrefix = sharedElementPrefix,
                sharedElementScope = sharedElementScope,
            )

            UnknownEmbed -> UnknownPostPost(onClick = {})
            is Video -> Unit


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
