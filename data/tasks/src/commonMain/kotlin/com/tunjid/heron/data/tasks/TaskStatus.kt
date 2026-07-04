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

/**
 * The observable status of a [Task]. Only four states are definitively knowable across every
 * platform. Success is deliberately not one of them: a finished download is simply [NotFound]
 * with its output file present on disk.
 */
sealed interface TaskStatus {
    /** Present in the datastore: the task has been asked for. */
    data object Created : TaskStatus

    /** The platform reports the task is actively running. */
    data class Running(
        val progress: Progress?,
    ) : TaskStatus

    /** A terminal failure recorded in the datastore from a platform callback. */
    data class Failed(
        val reason: String?,
    ) : TaskStatus

    /**
     * None of the above. The task may never have been created, or may have succeeded — in which
     * case its output file is on disk.
     */
    data object NotFound : TaskStatus
}

data class Progress(
    val completedBytes: Long,
    val totalBytes: Long,
) {
    /** Completion in `[0, 1]`, or `0` when the total size is unknown. */
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else completedBytes.toFloat() / totalBytes
}
