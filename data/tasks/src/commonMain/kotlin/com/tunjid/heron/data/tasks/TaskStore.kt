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

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Durable store of the tasks the app has asked for, and the write-port through which a
 * [BackgroundTaskScheduler] records terminal outcomes.
 *
 * It is the source of truth for [TaskStatus.Created] (a task is present in [pending]) and
 * [TaskStatus.Failed] (recorded in [failed]). Because background work runs out-of-band, no observer
 * of [BackgroundTaskScheduler.status] is guaranteed at the moment it finishes — so a scheduler calls
 * [markFailed] on failure and [remove] on success straight from its platform callback.
 *
 */
interface TaskStore {

    val pending: Flow<List<Task>>

    val failed: Flow<List<FailedTask>>

    /**
     * Records that [task] has been asked for. Returns `true` if it was newly added, or `false` if a
     * task with the same [Task.id] was already pending. Idempotent by [Task.id].
     */
    suspend fun add(
        task: Task,
    ): Boolean

    /** Drops [id] from both [pending] and [failed] — e.g. on success or cancellation. */
    suspend fun remove(
        id: TaskId,
    )

    /** Moves [id] from [pending] to [failed]. No-op if it is not pending. */
    suspend fun markFailed(
        id: TaskId,
        reason: String?,
    )
}

@Serializable
data class FailedTask(
    val task: Task,
    val reason: String?,
)
