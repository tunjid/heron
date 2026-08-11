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
package com.tunjid.heron.sheets.inference

import com.tunjid.heron.data.core.models.Image
import com.tunjid.heron.data.core.models.MediaItem
import com.tunjid.heron.data.core.models.MediaList
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.data.core.models.flattenedText
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.ml.language.englishDisplayName

internal fun translationPrompt(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
): String {
    val sourceLanguage = englishDisplayName(sourceLanguageTag)
    val targetLanguage = englishDisplayName(targetLanguageTag)
    return """
        Translate the text between <text> tags from $sourceLanguage to $targetLanguage.
        Reply with only the $targetLanguage translation: no quotes, no notes, no explanation.

        <text>
        $text
        </text>

        translation in $targetLanguage:
    """.trimIndent()
}

internal fun vibePrompt(
    items: List<TimelineItem>,
    profile: Profile,
    type: Timeline.Profile.Type,
): String = buildString {
    // The two lenses read a profile differently: their posts show the voice they broadcast in,
    // their replies show how they behave in conversation. Frame the ask and the samples to match.
    val basis: String
    val samplesHeader: String
    when (type) {
        Timeline.Profile.Type.Replies -> {
            basis = "how they show up in replies to other people — their conversational tone, " +
                "their wit, and how they treat the people they talk to"
            samplesHeader = "Recent replies, shown within the conversations they belong to " +
                "(the author's own lines are marked):"
        }
        else -> {
            basis = "their recent posts — the topics they gravitate to and the voice they post in"
            samplesHeader = "Recent posts:"
        }
    }
    appendLine(
        """
            Describe the "vibe" of a social media profile in two or three short sentences.
            Base it on the author's bio and $basis, including any content labels.
            Reply with only the description — no preamble, no headings, no quotes.
        """.trimIndent(),
    )
    appendLine()
    appendLine("Profile:")
    appendLine("- handle: @${profile.handle.id}")
    profile.displayName?.let { appendLine("- name: $it") }
    profile.description?.let { appendLine("- bio: $it") }
    profile.labels.takeIf { it.isNotEmpty() }?.let { labels ->
        appendLine("- labels: ${labels.joinToString { it.value.value }}")
    }
    appendLine()
    appendLine(samplesHeader)
    items.forEach { item ->
        appendVibeSample(
            item = item,
            authorId = profile.did,
        )
    }
}

internal fun teaPrompt(
    items: List<TimelineItem>,
): String = buildString {
    appendLine(
        """
            Sum up the "tea" (the gist) of this quote thread in two or three short sentences.
            A quote thread is a chain of posts where each post quotes the one before it,
            usually to carry a single train of thought forward.
            Say what the thread is about and where it lands, including any content labels.
            Reply with only the summary, no preamble, no headings, no quotes.
        """.trimIndent(),
    )
    appendLine()
    appendLine("Thread, oldest post first:")
    // postQuoteThread lists the chain from the anchor back to the post it ultimately quotes, so
    // reverse it to read the thread in the order it was written.
    items.asReversed()
        .map(TimelineItem::post)
        .filter { it.hasReadableContent }
        .forEachIndexed { index, post ->
            appendTeaPost(
                ordinal = index + 1,
                post = post,
            )
        }
}

/**
 * Appends one recent [item] as a vibe sample. Standalone posts render as a single line; reply and
 * quote threads render as an indented conversation so the model sees the context [authorId] is
 * responding to, with the author's own lines marked.
 */
private fun StringBuilder.appendVibeSample(
    item: TimelineItem,
    authorId: ProfileId,
) {
    when (item) {
        is TimelineItem.Single,
        is TimelineItem.Pinned,
        -> appendVibePost(
            prefix = "- ",
            post = item.post,
            authorId = authorId,
            withHandle = false,
        )

        is TimelineItem.Repost -> appendVibePost(
            prefix = "- reposted ",
            post = item.post,
            authorId = authorId,
            withHandle = true,
        )

        is TimelineItem.Threaded.Linear -> appendVibeConversation(
            posts = item.nodes.map { it.post },
            authorId = authorId,
        )

        is TimelineItem.Threaded.Tree -> appendVibeConversation(
            posts = listOf(item.anchor.post) + item.replies.map { it.post },
            authorId = authorId,
        )

        is TimelineItem.Placeholder -> Unit
    }
}

private fun StringBuilder.appendVibeConversation(
    posts: List<Post>,
    authorId: ProfileId,
) {
    if (posts.none { it.hasReadableContent }) return
    appendLine("- conversation:")
    posts.forEach { post ->
        appendVibePost(
            prefix = "    ",
            post = post,
            authorId = authorId,
            withHandle = true,
        )
    }
}

private fun StringBuilder.appendVibePost(
    prefix: String,
    post: Post,
    authorId: ProfileId,
    withHandle: Boolean,
) {
    if (!post.hasReadableContent) return
    append(prefix)
    if (withHandle) {
        append("@${post.author.handle.id}")
        if (post.author.did == authorId) append(" (the author)")
        append(": ")
    }
    append("\"${post.flattenedText.orEmpty()}\"")
    appendPostMetadata(post)
    appendLine()
}

private fun StringBuilder.appendTeaPost(
    ordinal: Int,
    post: Post,
) {
    append(ordinal)
    append(". @${post.author.handle.id}: ")
    append("\"${post.flattenedText.orEmpty()}\"")
    appendPostMetadata(post)
    appendLine()
}

/**
 * Appends a post's non-textual context — content labels and media alt text — as bracketed
 * metadata, so the model can factor in images and video it cannot otherwise see.
 */
private fun StringBuilder.appendPostMetadata(
    post: Post,
) {
    val labels = post.labels.joinToString { it.value.value }
    if (labels.isNotEmpty()) append(" [labels: $labels]")
    val altText = post.mediaAltText
    if (altText.isNotEmpty()) append(
        " [media alt text: ${altText.joinToString(separator = "; ") { "\"$it\"" }}]",
    )
}

private val Post.hasReadableContent: Boolean
    get() = !flattenedText.isNullOrEmpty() || labels.isNotEmpty() || mediaAltText.isNotEmpty()

private val Post.mediaAltText: List<String>
    get() = when (val embed = embed) {
        is MediaList -> embed.media.mapNotNull { it.altTextOrNull }
        is Video -> listOfNotNull(embed.alt)
        else -> emptyList()
    }.filter { it.isNotBlank() }

private val MediaItem.altTextOrNull: String?
    get() = when (this) {
        is Image -> alt
        is Video -> alt
    }
