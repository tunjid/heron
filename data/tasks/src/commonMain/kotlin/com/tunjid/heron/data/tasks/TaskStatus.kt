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

sealed interface TaskStatus {
    data object Created : TaskStatus

    data class Running(
        val progress: Progress?,
    ) : TaskStatus

    data class Failed(
        val reason: String?,
    ) : TaskStatus

    /**
     * None of the above. The task may have never been created, or may have succeeded.
     */
    data object NotFound : TaskStatus
}

data class Progress(
    val completedBytes: Long,
    val totalBytes: Long,
) {
    /** Completion in `[0, 1]`, or `0` when the total size is unknown. */
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}
