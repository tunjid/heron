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

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The single app-facing entry point for background work. It persists the tasks that have been asked
 * for (via [taskStore]) and hands execution to the platform.
 */
abstract class BackgroundTaskScheduler(
    internal val taskStore: TaskStore,
    internal val httpClient: HttpClient,
) {

    val tasks: Flow<List<Task>>
        get() = taskStore.pending

    suspend fun enqueue(
        task: Task,
    ) {
        // Only hand a genuinely new task to the platform; a duplicate id is already scheduled.
        if (taskStore.add(task)) schedule(task)
    }

    /**
     * [TaskStatus.Failed] and [TaskStatus.Created] come from [taskStore]; [TaskStatus.Running] is the
     * platform's live signal from [liveStatus]; anything else is [TaskStatus.NotFound] — a finished
     * download is simply absent from the store, with its file on disk.
     */
    fun status(
        id: TaskId,
    ): Flow<TaskStatus> =
        combine(
            taskStore.pending,
            taskStore.failed,
            liveStatus(id),
        ) { pending, failed, running ->
            val failedTask = failed.firstOrNull { it.task.id == id }
            when {
                failedTask != null -> TaskStatus.Failed(failedTask.reason)
                running != null -> running
                pending.any { it.id == id } -> TaskStatus.Created
                else -> TaskStatus.NotFound
            }
        }
            .distinctUntilChanged()

    suspend fun cancel(
        id: TaskId,
    ) {
        cancelScheduled(id)
        taskStore.remove(id)
    }

    /** Hand [task] to the OS for out-of-band execution. */
    protected abstract suspend fun schedule(
        task: Task,
    )

    /** Emits [TaskStatus.Running] (with progress) while the platform is actively running [id], or `null` otherwise. */
    protected abstract fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?>

    /** Cancels any in-flight platform work for [id]; returns `true` if something was actually cancelled. */
    protected abstract suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean
}
