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
import com.tunjid.heron.data.files.path
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** What a running transfer wants shown in its notification. */
internal data class TransferNotice(
    val channelId: String,
    val title: String,
    val smallIcon: Int,
    val progress: Progress?,
)

/**
 * Runs the task for [id] on whichever OS component invoked it (worker or job service). A download is
 * streamed here; an upload is sent by the write queue instead, so this only holds the process open
 * for it.
 */
internal suspend fun Context.runTransfer(
    id: TaskId,
    onNotice: suspend (TransferNotice) -> Unit,
): Result<Unit> {
    val scheduler = backgroundTaskScheduler
    val taskStore = scheduler.taskStore

    if (scheduler.isHoldingProcess(id).first()) {
        onNotice(UploadNotice)
        withTimeoutOrNull(ProcessHoldTimeout) {
            scheduler.isHoldingProcess(id).first { holding -> !holding }
        }
        return Result.success(Unit)
    }

    // An absent task has already finished; reporting failure here would ask for a needless retry.
    val task = taskStore.pending
        .first()
        .firstOrNull { it.id == id }
        ?: return Result.success(Unit)

    return try {
        when (task) {
            is Task.Download -> {
                onNotice(task.notice(progress = Progress(0L, task.sizeInBytes)))
                scheduler.download(
                    request = task,
                    authHeader = null, // TODO: resolve a gated-host bearer token (e.g. Hugging Face) at run time.
                    onProgress = { progress -> onNotice(task.notice(progress = progress)) },
                )
                taskStore.remove(id)
            }
            // A hold is never put in the store, so it cannot reach here; it is handled above.
            is Task.Upload -> Unit
        }
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

internal fun Task.notice(
    progress: Progress?,
): TransferNotice = when (this) {
    is Task.Download -> TransferNotice(
        channelId = kind.channelId,
        title = destination.path.name,
        smallIcon = android.R.drawable.stat_sys_download,
        progress = progress,
    )
    is Task.Upload -> UploadNotice
}

/** The notice for [id] before it reports one of its own; a task already gone shows the default. */
internal suspend fun Context.pendingNotice(
    id: TaskId,
): TransferNotice {
    val scheduler = backgroundTaskScheduler
    if (scheduler.isHoldingProcess(id).first()) return UploadNotice
    return scheduler.taskStore.pending
        .first()
        .firstOrNull { it.id == id }
        ?.notice(progress = null)
        ?: DownloadNotice
}

private val DownloadNotice = TransferNotice(
    channelId = Task.Kind.Transfer.channelId,
    title = "Download",
    smallIcon = android.R.drawable.stat_sys_download,
    progress = null,
)

private val UploadNotice = TransferNotice(
    channelId = Task.Kind.Upload.channelId,
    title = "Uploading media",
    smallIcon = android.R.drawable.stat_sys_upload,
    progress = null,
)

private val ProcessHoldTimeout = 15.minutes
