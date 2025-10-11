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

package com.tunjid.heron.scaffold.scaffold

import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.ui.text.Memo
import heron.scaffold.generated.resources.Res
import heron.scaffold.generated.resources.bookmark
import heron.scaffold.generated.resources.bookmark_removal
import heron.scaffold.generated.resources.duplicate_post_interaction
import heron.scaffold.generated.resources.failed_post_interaction
import heron.scaffold.generated.resources.like
import heron.scaffold.generated.resources.repost
import heron.scaffold.generated.resources.repost_removal
import heron.scaffold.generated.resources.unlike
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun viewModelCoroutineScope() = CoroutineScope(
    SupervisorJob() + Dispatchers.Main.immediate,
)

fun Post.Interaction.duplicateWriteMessage() = Memo.Resource(
    stringResource = Res.string.duplicate_post_interaction,
    args = listOf(
        when (this) {
            is Post.Interaction.Create.Bookmark -> Res.string.bookmark
            is Post.Interaction.Create.Like -> Res.string.like
            is Post.Interaction.Create.Repost -> Res.string.repost
            is Post.Interaction.Delete.RemoveBookmark -> Res.string.bookmark_removal
            is Post.Interaction.Delete.RemoveRepost -> Res.string.repost_removal
            is Post.Interaction.Delete.Unlike -> Res.string.unlike
        },
    ),
)

fun Post.Interaction.failedWriteMessage() = Memo.Resource(
    stringResource = Res.string.failed_post_interaction,
)

internal val FabSharedElementZIndex = 4f
