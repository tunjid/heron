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

package com.tunjid.heron.conversation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.FeedGenerator
import com.tunjid.heron.data.core.models.FeedList
import com.tunjid.heron.data.core.models.Labeler
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Record
import com.tunjid.heron.data.core.models.StarterPack
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.timeline.ui.PostActions
import com.tunjid.heron.timeline.ui.feed.FeedGenerator
import com.tunjid.heron.timeline.ui.label.Labeler
import com.tunjid.heron.timeline.ui.list.FeedList
import com.tunjid.heron.timeline.ui.list.StarterPack
import com.tunjid.heron.timeline.ui.post.feature.QuotedPost
import kotlinx.datetime.Clock

@Composable
internal fun MessageRecord(
    modifier: Modifier = Modifier,
    record: Record,
    sharedElementPrefix: String,
    paneScaffoldState: PaneScaffoldState,
    postActions: PostActions,
) {
    OutlinedCard(
        modifier = modifier,
    ) {
        when (record) {
            is Labeler -> Labeler(
                modifier = NonPostRecordModifier,
                sharedElementPrefix = sharedElementPrefix,
                movableElementSharedTransitionScope = paneScaffoldState,
                labeler = record,
            )
            is Post -> QuotedPost(
                paneMovableElementSharedTransitionScope = paneScaffoldState,
                now = remember { Clock.System.now() },
                quotedPost = record,
                isBlurred = false,
                sharedElementPrefix = sharedElementPrefix,
                onClick = {
                    postActions.onPostClicked(post = record)
                },
                onLinkTargetClicked = postActions::onLinkTargetClicked,
                onProfileClicked = { post, profile ->
                    postActions.onProfileClicked(
                        profile = profile,
                        post = post,
                        quotingPostUri = null,
                    )
                },
                onPostMediaClicked = { mediaEmbed, index, post ->
                    postActions.onPostMediaClicked(
                        media = mediaEmbed,
                        index = index,
                        post = post,
                        quotingPostUri = null,
                    )
                },
            )
            is FeedGenerator -> FeedGenerator(
                modifier = NonPostRecordModifier,
                sharedElementPrefix = sharedElementPrefix,
                movableElementSharedTransitionScope = paneScaffoldState,
                feedGenerator = record,
                status = null,
                onFeedGeneratorStatusUpdated = {},
            )
            is FeedList -> FeedList(
                modifier = NonPostRecordModifier,
                sharedElementPrefix = sharedElementPrefix,
                movableElementSharedTransitionScope = paneScaffoldState,
                list = record,
            )
            is StarterPack -> StarterPack(
                modifier = NonPostRecordModifier,
                sharedElementPrefix = sharedElementPrefix,
                movableElementSharedTransitionScope = paneScaffoldState,
                starterPack = record,
            )
        }
    }
}

private val NonPostRecordModifier = Modifier
    .padding(12.dp)
