/*
 *    Copyright 2026 Adetunji Dahunsi
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

package com.tunjid.heron.messages

import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.types.ProfileHandle
import com.tunjid.heron.data.core.types.ProfileId

/**
 * Sample data for the messages-list [@Preview][androidx.compose.ui.tooling.preview.Preview]
 * composables in this module.
 */
internal object PreviewFixtures {

    data class ConversationRow(
        val profile: Profile,
        val summary: String,
    )

    val conversations: List<ConversationRow> = listOf(
        ConversationRow(
            profile = profile("Julian Marinus", "fooljulian.bsky.social"),
            summary = "Yuri Schimke reacted ❤️ to \"Got the invite, thanks!\"",
        ),
        ConversationRow(
            profile = profile("Anton Arhipov", "antonarhipov.bsky.social"),
            summary = "Hey, Mark, can we move our sync to half past three? I want to " +
                "walk through the new build pipeline and the preview rendering " +
                "changes before the team standup tomorrow morning.",
        ),
        ConversationRow(
            profile = profile("Jesse Wilson", "swank.ca"),
            summary = "Yup. And that wraps up our time, you can stop sharing.",
        ),
    )

    fun profile(
        displayName: String,
        handle: String,
    ): Profile = Profile(
        did = ProfileId(handle),
        handle = ProfileHandle(handle),
        displayName = displayName,
        description = null,
        avatar = null,
        banner = null,
        followersCount = 0,
        followsCount = 0,
        postsCount = 0,
        joinedViaStarterPack = null,
        indexedAt = null,
        createdAt = null,
        metadata = Profile.Metadata(
            createdListCount = 0,
            createdFeedGeneratorCount = 0,
            createdStarterPackCount = 0,
            chat = Profile.ChatInfo(
                allowed = Profile.ChatInfo.Allowed.NoOne,
            ),
        ),
    )
}
