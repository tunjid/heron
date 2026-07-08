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

import com.tunjid.heron.data.files.FileManager
import io.ktor.client.HttpClient
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Desktop [BackgroundTaskScheduler]: the process is long-lived, so transfers run in-process on
 * [scope] via the shared [download] extension. A killed process loses the coroutine, but the
 * persisted [TaskStore] re-enqueues pending tasks on the next launch and the on-disk `.part` file
 * lets the download resume from where it stopped.
 */
internal class DesktopBackgroundTaskScheduler(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    httpClient: HttpClient,
    fileManager: FileManager,
    taskStore: TaskStore,
) : BackgroundTaskScheduler(taskStore, httpClient, fileManager) {

    private val mutex = Mutex()
    private val jobs = mutableMapOf<TaskId, Job>()

    // Presence of an id => the platform (here, our coroutine) is actively running it.
    private val progresses = MutableStateFlow<Map<TaskId, Progress?>>(emptyMap())

    override suspend fun schedule(
        task: Task,
    ) {
        if (task !is Task.Download) return
        mutex.withLock {
            if (jobs.containsKey(task.id)) return
            jobs[task.id] = scope.launch(ioDispatcher) {
                try {
                    // TODO: resolve a gated-host bearer token (e.g. Hugging Face) at run time.
                    download(
                        request = task,
                        authHeader = null,
                        onProgress = { progress -> progresses.update { it + (task.id to progress) } },
                    )
                    taskStore.remove(task.id)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    taskStore.markFailed(task.id, throwable.message)
                } finally {
                    withContext(NonCancellable) {
                        mutex.withLock { jobs.remove(task.id) }
                        progresses.update { it - task.id }
                    }
                }
            }
        }
    }

    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> =
        progresses
            .map { current ->
                if (current.containsKey(id)) TaskStatus.Running(current[id]) else null
            }
            .distinctUntilChanged()

    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean {
        val job = mutex.withLock { jobs.remove(id) }
        job?.cancelAndJoin()
        progresses.update { it - id }
        return job != null
    }
}

fun createBackgroundTaskScheduler(
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    fileManager: FileManager,
    taskStore: TaskStore,
    httpClient: HttpClient,
): BackgroundTaskScheduler = DesktopBackgroundTaskScheduler(
    scope = scope,
    ioDispatcher = ioDispatcher,
    httpClient = httpClient,
    fileManager = fileManager,
    taskStore = taskStore,
)
