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

package com.tunjid.heron.data.tasks

import android.content.Context
import com.tunjid.heron.data.tasks.TransferNotifications.ensureChannel
import kotlinx.coroutines.flow.first

internal suspend fun Context.runTransfer(
    id: TaskId,
    onProgress: suspend (description: TaskDescription, progress: Progress?) -> Unit,
): Result<Unit> {
    val task = pendingTask(id)
        ?: return Result.failure(IllegalStateException("No pending task for ${id.value}"))
    ensureChannel()
    val description = backgroundTaskDescriptor.describe(task)
    onProgress(description, null)
    return backgroundTaskRunner.run(id) { progress ->
        onProgress(description, progress)
    }
}

internal suspend fun Context.describeTransfer(
    id: TaskId,
): TaskDescription {
    ensureChannel()
    return pendingTask(id)
        ?.let { backgroundTaskDescriptor.describe(it) }
        ?: TaskDescription(
            title = backgroundTaskDescriptor.channelName(),
            subtitle = null,
            destination = null,
        )
}

private suspend fun Context.pendingTask(
    id: TaskId,
): Task? = backgroundTaskScheduler.taskStore.pending
    .first()
    .firstOrNull { it.id == id }
