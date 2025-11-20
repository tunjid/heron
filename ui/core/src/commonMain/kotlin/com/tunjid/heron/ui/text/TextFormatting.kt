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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.em
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import heron.ui.core.generated.resources.Res

typealias CommonStrings = Res.string

@Composable
fun rememberFormattedTextPost(
    text: String,
    textLinks: List<Link>,
    textLinkStyles: TextLinkStyles? = null,
    onLinkTargetClicked: (LinkTarget) -> Unit = NoOpLinkTargetHandler,
): AnnotatedString = remember(text) {
    formatTextPost(
        text = text,
        textLinks = textLinks,
        textLinkStyles = textLinkStyles,
        onLinkTargetClicked = onLinkTargetClicked,
    )
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
    textLinks: List<Link>,
    textLinkStyles: TextLinkStyles? = null,
    onLinkTargetClicked: (LinkTarget) -> Unit = NoOpLinkTargetHandler,
): AnnotatedString = buildAnnotatedString {
    append(text)

    val newlineIndices = text.indices.filter { text[it] == '\n' }
    newlineIndices.forEach { index ->
        addStyle(
            style = ParagraphStyle(lineHeight = 0.1.em),
            start = index,
            end = index + 1,
        )
        addStyle(
            style = SpanStyle(fontSize = 0.1.em),
            start = index,
            end = index + 1,
        )
    }

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
                is LinkTarget.ExternalLink -> {
                    addLink(
                        url = LinkAnnotation.Url(
                            url = target.uri.uri,
                            styles = textLinkStyles,
                        ),
                        start = start,
                        end = end,
                    )
                }

                is LinkTarget.Hashtag -> {
                    addLink(
                        clickable = LinkAnnotation.Clickable(target.tag) {
                            onLinkTargetClicked(target)
                        },
                        start = start,
                        end = end,
                    )
                }

                is LinkTarget.UserDidMention -> {
                    addLink(
                        clickable = LinkAnnotation.Clickable(target.did.id) {
                            onLinkTargetClicked(target)
                        },
                        start = start,
                        end = end,
                    )
                }

                is LinkTarget.UserHandleMention -> {
                    addLink(
                        clickable = LinkAnnotation.Clickable(target.handle.id) {
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
