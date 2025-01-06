package com.tunjid.heron.timeline.ui.post

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ExternalEmbed
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.stubProfile
import com.tunjid.heron.ui.SharedElementScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostText(
    post: Post,
    sharedElementPrefix: String,
    sharedElementScope: SharedElementScope,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onProfileClicked: (Post, Profile) -> Unit,
) = with(sharedElementScope) {
    val maybeExternalLink = (post.embed as? ExternalEmbed)?.uri?.uri
    val text = post.record?.text?.removeSuffix(maybeExternalLink.orEmpty())?.trim().orEmpty()

    if (text.isBlank()) Spacer(Modifier.height(0.dp))
    else Text(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .sharedElement(
                key = post.textSharedElementKey(
                    prefix = sharedElementPrefix,
                ),
            ),
        text = rememberFormattedTextPost(
            text = text,
            textLinks = post.record?.links ?: emptyList(),
            onProfileClicked = { onProfileClicked(post, it) }
        ),
        style = LocalTextStyle.current.copy(color = LocalContentColor.current),
    )
}

@Composable
fun rememberFormattedTextPost(
    text: String,
    textLinks: List<Post.Link>,
    onProfileClicked: (Profile) -> Unit,
): AnnotatedString = remember(text) {
    formatTextPost(
        text = text,
        textLinks = textLinks,
        onProfileClicked = onProfileClicked,
    )
}

fun formatTextPost(
    text: String,
    textLinks: List<Post.Link>,
    onProfileClicked: (Profile) -> Unit,
): AnnotatedString {
    return buildAnnotatedString {
        append(text)

        val byteOffsets = text.byteOffsets()
        textLinks.forEach { link ->
            if (link.start < byteOffsets.size && link.end < byteOffsets.size) {
                val start = byteOffsets[link.start]
                val end = byteOffsets[link.end]

                addStyle(
                    style = SpanStyle(color = Color(0xFF3B62FF)),
                    start = start,
                    end = end,
                )

                when (val target = link.target) {
                    is Post.LinkTarget.ExternalLink -> {
                        addLink(
                            url = LinkAnnotation.Url(target.uri.uri),
                            start = start,
                            end = end,
                        )
                    }

                    is Post.LinkTarget.Hashtag -> {
                        addLink(
                            clickable = LinkAnnotation.Clickable(target.tag) {

                            },
                            start = start,
                            end = end,
                        )
                    }

                    is Post.LinkTarget.UserDidMention -> {
                        addLink(
                            clickable = LinkAnnotation.Clickable(target.did.id) {
                                onProfileClicked(
                                    stubProfile(
                                        did = target.did,
                                        handle = target.did,
                                    )
                                )
                            },
                            start = start,
                            end = end,
                        )
                    }

                    is Post.LinkTarget.UserHandleMention -> {
                        addLink(
                            clickable = LinkAnnotation.Clickable(target.handle.id) {
                                onProfileClicked(
                                    stubProfile(
                                        did = target.handle,
                                        handle = target.handle,
                                    )
                                )
                            },
                            start = start,
                            end = end,
                        )
                    }
                }
            }
        }
    }
}

private fun Post.textSharedElementKey(
    prefix: String,
): String = "$prefix-${cid.id}-text"

private fun String.byteOffsets(): List<Int> = buildList {
    var i = 0
    var lastWas4Bytes = false

    while (i < length) {
        lastWas4Bytes = false
        val c = this@byteOffsets[i].code

        if (c < 0x80) {
            // A 7-bit character with 1 byte.
            repeat(1) { add(i) }
            i++
        } else if (c < 0x800) {
            // An 11-bit character with 2 bytes.
            repeat(2) { add(i) }
            i++
        } else if (c < 0xD800 || c > 0xDFFF) {
            // A 16-bit character with 3 bytes.
            repeat(3) { add(i) }
            i++
        } else {
            val low = if (i + 1 < length) this@byteOffsets[i + 1].code else 0

            if (c > 0xDBFF || low < 0xDC00 || low > 0xDFFF) {
                // A malformed surrogate, which yields '?'.
                repeat(1) { add(i) }
                i++
            } else {
                // A 21-bit character with 4 bytes.
                repeat(4) { add(i) }
                i += 2
                lastWas4Bytes = true
            }
        }
    }
    if (isNotEmpty()) {
        if (lastWas4Bytes) add(i - 1) else add(i)
    }
}