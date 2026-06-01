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

import androidx.compose.ui.text.AnnotatedString
import com.tunjid.heron.data.core.models.Link
import com.tunjid.heron.data.core.models.LinkTarget
import com.tunjid.heron.data.core.types.GenericUri
import com.tunjid.heron.data.core.types.ProfileHandle

fun AnnotatedString.links(): List<Link> {
    val byteOffsets = text.byteOffsets()

    val mentions =
        handleRegex.findAll(text).map {
            Link(
                start = byteOffsets.indexOf(it.range.first),
                end = byteOffsets.indexOf(it.range.last + 1),
                // Ok this is actually a handle for now, but it is resolved to a Did later on.
                target = LinkTarget.UserHandleMention(ProfileHandle(it.groupValues[3])),
            )
        }

    val hashtags =
        hashtagRegex.findAll(text).map {
            Link(
                start = byteOffsets.indexOf(it.range.first),
                end = byteOffsets.indexOf(it.range.last + 1),
                target = LinkTarget.Hashtag(it.groupValues[3]),
            )
        }

    val hyperlinks =
        hyperlinkRegex.findAll(text).map {
            var url = it.groupValues[2]
            if (!url.startsWith("http")) {
                url = "https://$url"
            }
            url = url.dropLastWhile { c -> c in ".,;!?" }
            if (url.endsWith(')') && '(' !in url) {
                url = url.dropLast(1)
            }

            Link(
                start = byteOffsets.indexOf(it.range.first),
                end = byteOffsets.indexOf(it.range.last + 1),
                target = LinkTarget.ExternalLink(GenericUri(url)),
            )
        }

    return (mentions + hashtags + hyperlinks).toList()
}

private val handleRegex = Regex("(^|\\s|\\()(@)([a-zA-Z0-9.-]+)(\\b)")

private val hashtagRegex = Regex("(^|\\s|\\()(#)([a-zA-Z0-9]+)(\\b)")

private val hyperlinkRegex =
    Regex(
        "(^|\\s|\\()((https?://\\S+)|(([a-z][a-z0-9]*(\\.[a-z0-9]+)+)\\S*))",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
