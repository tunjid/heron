/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.ui.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import heron.ui.core.generated.resources.Res

typealias CommonStrings = Res.string

@Composable
fun rememberFormattedTextPost(
    text: String,
    textLinks: List<Link> = emptyList(),
    textLinkStyles: TextLinkStyles? = null,
    onLinkTargetClicked: (LinkTarget) -> Unit = NoOpLinkTargetHandler,
): AnnotatedString {
    val updatedOnLinkTargetClicked by rememberUpdatedState(onLinkTargetClicked)
    val linkColor = MaterialTheme.colorScheme.primary
    val linkStyles = textLinkStyles ?: remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.None,
            ),
        )
    }
    return remember(text, textLinks, linkStyles) {
        formatTextPost(
            text = text,
            textLinks = textLinks,
            textLinkStyles = linkStyles,
            onLinkTargetClicked = {
                updatedOnLinkTargetClicked(it)
            },
        )
    }
}

@Composable
fun MultilineStyledText(
    modifier: Modifier = Modifier,
    text: String,
    links: List<Link>,
    linkStyles: TextLinkStyles? = null,
    onLinkTargetClicked: (LinkTarget) -> Unit,
    maxLines: Int = Int.MAX_VALUE,
) {
    val formattedText = rememberFormattedTextPost(
        text = text,
        textLinks = links,
        textLinkStyles = linkStyles,
        onLinkTargetClicked = onLinkTargetClicked,
    )
    val style = MaterialTheme.typography.bodyLarge.copy(
        color = LocalContentColor.current,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    SelectionContainer(
        modifier,
    ) {
        // Truncated text stays a single Text so maxLines and the ellipsis apply across it.
        if (maxLines != Int.MAX_VALUE) Text(
            text = formattedText,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = style,
        )
        else Column(
            verticalArrangement = Arrangement.spacedBy(ParagraphSpacing),
        ) {
            val paragraphs = remember(formattedText) {
                formattedText.paragraphs()
            }
            paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = style,
                )
            }
        }
    }
}

/**
 * Splits text into paragraphs at blank lines, so the gap between them can be set explicitly.
 * Blank lines can't be shortened inside a single Text on every platform: on Skia (iOS, desktop)
 * a line is never shorter than the font's natural height.
 */
private fun AnnotatedString.paragraphs(): List<AnnotatedString> {
    val paragraphs = mutableListOf<AnnotatedString>()
    var start = 0
    BlankLinesRegex.findAll(text).forEach { match ->
        if (match.range.first > start) paragraphs.add(subSequence(start, match.range.first))
        start = match.range.last + 1
    }
    if (start < length) paragraphs.add(subSequence(start, length))
    return paragraphs
}

fun TextFieldValue.withFormattedTextPost(
    textLinkStyles: TextLinkStyles? = null,
) = copy(
    annotatedString = formatTextPost(
        text = text,
        textLinks = annotatedString.links(),
        textLinkStyles = textLinkStyles,
        onLinkTargetClicked = NoOpLinkTargetHandler,
    ),
)

fun formatTextPost(
    text: String,
    textLinks: List<Link> = emptyList(),
    textLinkStyles: TextLinkStyles? = null,
    onLinkTargetClicked: (LinkTarget) -> Unit = NoOpLinkTargetHandler,
): AnnotatedString = buildAnnotatedString {
    append(text)

    val linkStyles = textLinkStyles ?: DefaultTextLinkStyles
    val byteOffsets = text.byteOffsets()
    textLinks.forEach { link ->
        if (link.start < byteOffsets.size && link.end < byteOffsets.size && link.start < link.end) {
            val start = byteOffsets[link.start]
            val end = byteOffsets[link.end]

            // Without explicit link styles (e.g. the composer's TextFieldValue), color the span directly.
            if (textLinkStyles == null) addStyle(
                style = SpanStyle(color = Color(0xFF3B62FF)),
                start = start,
                end = end,
            )

            when (val target = link.target) {
                is LinkTarget.ExternalLink -> {
                    addLink(
                        url = LinkAnnotation.Url(
                            url = target.uri.uri,
                            styles = linkStyles,
                        ),
                        start = start,
                        end = end,
                    )
                }

                is LinkTarget.Hashtag -> {
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = target.tag,
                            styles = linkStyles,
                        ) {
                            onLinkTargetClicked(target)
                        },
                        start = start,
                        end = end,
                    )
                }

                is LinkTarget.UserDidMention -> {
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = target.did.id,
                            styles = linkStyles,
                        ) {
                            onLinkTargetClicked(target)
                        },
                        start = start,
                        end = end,
                    )
                }

                is LinkTarget.UserHandleMention -> {
                    addLink(
                        clickable = LinkAnnotation.Clickable(
                            tag = target.handle.id,
                            styles = linkStyles,
                        ) {
                            onLinkTargetClicked(target)
                        },
                        start = start,
                        end = end,
                    )
                }
            }
        }
    }
}

/**
 * Returns a mapping of byte offsets to character offsets.
 * Assumes that you are providing a valid UTF-8 string as input.
 * Text encodings are really a lot of fun.
 */
internal fun String.byteOffsets(): List<Int> = buildList {
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

private val NoOpLinkTargetHandler: (LinkTarget) -> Unit = {}

// Links are distinguished by color alone, without the default underline.
private val DefaultTextLinkStyles = TextLinkStyles(
    style = SpanStyle(
        textDecoration = TextDecoration.None,
    ),
)

private val BlankLinesRegex = Regex("\\n[ \\t]*\\n\\s*")

private val ParagraphSpacing = 6.dp
