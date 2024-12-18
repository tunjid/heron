package com.tunjid.heron.feed.ui

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
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.feed.ui.feature.BlockedPostPost
import com.tunjid.heron.feed.ui.feature.InvisiblePostPost
import com.tunjid.heron.feed.ui.feature.PostExternal
import com.tunjid.heron.feed.ui.feature.PostImages
import com.tunjid.heron.feed.ui.feature.UnknownPostPost
import com.tunjid.heron.feed.ui.feature.VisiblePostPost
import kotlinx.datetime.Instant

@Composable
internal fun PostEmbed(
    now: Instant,
    embed: Embed?,
    quote: Post?,
    onOpenImage: (Uri) -> Unit,
    onOpenPost: (Post) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column {
        when (embed) {
            is ExternalEmbed -> PostExternal(embed, onClick = {
                uriHandler.openUri(embed.uri.uri)
            })

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
                    onClick = {
//                    onOpenPost(ThreadProps.FromReference(embedPost.reference))
                    }
                )
            }
        }
    }
}
