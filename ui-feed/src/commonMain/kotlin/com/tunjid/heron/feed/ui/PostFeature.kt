package com.tunjid.heron.feed.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.UnknownEmbed
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.types.Uri
import kotlinx.datetime.Instant

@Composable
internal fun PostFeature(
    now: Instant,
    post: Post?,
    onOpenImage: (Uri) -> Unit,
    onOpenPost: (Post) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    when (post?.embed) {
        is ExternalEmbed -> Unit
        is ImageList -> Unit
        UnknownEmbed -> Unit
        is Video -> Unit
//        is ImagesFeature -> PostImages(post, onOpenImage)
//        is ExternalFeature -> PostExternal(post, onClick = {
//            uriHandler.openUri(post.uri.uri)
//        })
//
//        is PostFeature -> when (val embedPost = post.post) {
//            is EmbedPost.VisibleEmbedPost -> {
//                VisiblePostPost(now, embedPost.litePost, embedPost.author, onClick = {
//                    onOpenPost(ThreadProps.FromReference(embedPost.reference))
//                })
//            }
//
//            is EmbedPost.InvisibleEmbedPost -> InvisiblePostPost(onClick = {})
//            is EmbedPost.BlockedEmbedPost -> BlockedPostPost(onClick = {})
//            is EmbedPost.UnknownEmbedPost -> UnknownPostPost(onClick = {})
//        }
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
