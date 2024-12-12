package com.tunjid.heron.feed.ui

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
    onOpenImage: (Uri) -> Unit,
    onOpenPost: (Post) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    when (embed) {
        is ExternalEmbed -> PostExternal(embed, onClick = {
            uriHandler.openUri(embed.uri.uri)
        })

        is ImageList -> PostImages(embed)
        UnknownEmbed -> UnknownPostPost(onClick = {})
        is Video -> Unit

        is Post -> when (embed.cid) {
            Constants.notFoundPostId -> InvisiblePostPost(onClick = {})
            Constants.blockedPostId -> BlockedPostPost(onClick = {})
            Constants.unknownPostId -> UnknownPostPost(onClick = {})
            else -> {
                VisiblePostPost(now, embed, embed.author, onClick = {
//                    onOpenPost(ThreadProps.FromReference(embedPost.reference))
                })
            }

        }
//
//        is MediaPostFeature -> {
//            when (val embedMedia = post.media) {
//                is ImagesFeature -> PostImages(embedMedia, onOpenImage)
//                is ExternalFeature -> PostExternal(embedMedia, onClick = {
//                    uriHandler.openUri(embedMedia.uri.uri)
//                })
//            }
//            when (val embedPost = post.post) {
//                is EmbedPost.VisibleEmbedPost -> {
//                    VisiblePostPost(now, embedPost.litePost, embedPost.author, onClick = {
//                        onOpenPost(ThreadProps.FromReference(embedPost.reference))
//                    })
//                }
//
//                is EmbedPost.InvisibleEmbedPost -> InvisiblePostPost(onClick = {})
//                is EmbedPost.BlockedEmbedPost -> BlockedPostPost(onClick = {})
//                is EmbedPost.UnknownEmbedPost -> UnknownPostPost(onClick = {})
//            }
//        }

        null -> Unit
    }
}
