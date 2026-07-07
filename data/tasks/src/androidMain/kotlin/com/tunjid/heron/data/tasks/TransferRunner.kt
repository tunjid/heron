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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath

/**
 * Runs the download for [id] on whichever OS component invoked it (worker or job service).
 */
internal suspend fun Context.runTransfer(
    id: TaskId,
    onProgress: suspend (Task.Download, Progress) -> Unit,
): Result<Unit> {
    val scheduler = backgroundTaskScheduler
    val taskStore = scheduler.taskStore
    val task = taskStore.pending
        .first()
        .firstOrNull { it.id == id } as? Task.Download
        ?: return Result.failure(
            IllegalStateException("No pending download for ${id.value}"),
        )

    return try {
        onProgress(
            task,
            Progress(0L, task.sizeInBytes),
        )
        scheduler.download(
            request = task,
            destination = task.destination.toPath(),
            authHeader = null, // TODO: resolve a gated-host bearer token (e.g. Hugging Face) at run time.
            onProgress = { progress -> onProgress(task, progress) },
        )
        taskStore.remove(id)
        Result.success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        taskStore.markFailed(
            id = id,
            reason = throwable.message,
        )
        Result.failure(throwable)
    }
}
