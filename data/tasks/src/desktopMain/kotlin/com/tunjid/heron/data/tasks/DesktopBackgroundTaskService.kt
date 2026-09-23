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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object DesktopBackgroundTaskService {

    private lateinit var host: BackgroundTaskHost
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val jobs = mutableMapOf<TaskId, Job>()

    // Presence of an id => a coroutine here is actively running it.
    private val progresses = MutableStateFlow<Map<TaskId, Progress?>>(emptyMap())

    /** Installs the process [BackgroundTaskHost]. Call once at startup, before any task is scheduled. */
    fun install(
        host: BackgroundTaskHost,
    ) {
        this.host = host
        // Resume anything a previous run left pending; a killed process loses the coroutines, but the
        // durable TaskStore does not. run() dedupes, so this is safe alongside the write queue's drain.
        scope.launch {
            host.backgroundTaskScheduler.taskStore.pending
                .first()
                .forEach { run(it.id) }
        }
    }

    suspend fun run(
        id: TaskId,
    ) {
        mutex.withLock {
            if (jobs.containsKey(id)) return
            jobs[id] = scope.launch {
                try {
                    host.backgroundTaskRunner.run(
                        id = id,
                        onProgress = { progress -> progresses.update { it + (id to progress) } },
                    )
                } finally {
                    withContext(NonCancellable) {
                        mutex.withLock { jobs.remove(id) }
                        progresses.update { it - id }
                    }
                }
            }
        }
    }

    fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> =
        progresses
            .map { current ->
                if (current.containsKey(id)) TaskStatus.Running(current[id]) else null
            }
            .distinctUntilChanged()

    suspend fun cancel(
        id: TaskId,
    ): Boolean {
        val job = mutex.withLock { jobs.remove(id) }
        job?.cancelAndJoin()
        progresses.update { it - id }
        return job != null
    }
}
