package com.tunjid.heron.feed.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun PostText(
    post: Post,
    onClick: () -> Unit,
    onProfileClicked: Post?.(Profile) -> Unit,
) {
    val maybeExternalLink = (post.embed as? ExternalEmbed)?.uri?.uri
    val text = post.record?.text?.removeSuffix(maybeExternalLink.orEmpty())?.trim().orEmpty()

    if (text.isBlank()) {
        Spacer(Modifier.height(0.dp))
    } else {
        val postText = rememberFormattedTextPost(
            text = text,
//            post.textLinks,
        )

        val uriHandler = LocalUriHandler.current
        ClickableText(
            text = postText,
            style = LocalTextStyle.current.copy(color = LocalContentColor.current),
            onClick = { index ->
                var performedAction = false
                postText.getStringAnnotations("hashtag", index, index)
                    .firstOrNull()?.item?.let { hashtag ->
                    // TODO handle hashtag click
                }
                postText.getStringAnnotations("did", index, index).firstOrNull()?.item?.let { did ->
                    performedAction = true
//                    onOpenUser(UserDid(Did(did)))
                }
                postText.getStringAnnotations("handle", index, index)
                    .firstOrNull()?.item?.let { handle ->
                    performedAction = true
//                    onOpenUser(UserHandle(Handle(handle)))
                }
                postText.getUrlAnnotations(index, index).firstOrNull()?.item?.url?.let { url ->
                    performedAction = true
                    uriHandler.openUri(url)
                }
                if (!performedAction) {
                    onClick()
                }
            },
        )
    }
}

@Composable
fun rememberFormattedTextPost(
    text: String,
//    textLinks: List<TimelinePostLink>,
): AnnotatedString {
    return remember(text, ) {
        formatTextPost(
            text,
//            textLinks,
        )
    }
}

@OptIn(ExperimentalTextApi::class)
fun formatTextPost(
    text: String,
//    textLinks: List<TimelinePostLink>,
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

//        val byteOffsets = text.byteOffsets()
//        textLinks.forEach { link ->
//            if (link.start < byteOffsets.size && link.end < byteOffsets.size) {
//                val start = byteOffsets[link.start]
//                val end = byteOffsets[link.end]
//
//                addStyle(
//                    style = SpanStyle(color = Color(0xFF3B62FF)),
//                    start = start,
//                    end = end,
//                )
//
//                when (link.target) {
//                    is LinkTarget.ExternalLink -> {
//                        addUrlAnnotation(UrlAnnotation(link.target.uri.uri), start, end)
//                    }
//
//                    is LinkTarget.Hashtag -> {
//                        addStringAnnotation("hashtag", link.target.tag, start, end)
//                    }
//
//                    is LinkTarget.UserDidMention -> {
//                        addStringAnnotation("did", link.target.did.did, start, end)
//                    }
//
//                    is LinkTarget.UserHandleMention -> {
//                        addStringAnnotation("handle", link.target.handle.handle, start, end)
//                    }
//                }
//            }
//        }
    }
}
