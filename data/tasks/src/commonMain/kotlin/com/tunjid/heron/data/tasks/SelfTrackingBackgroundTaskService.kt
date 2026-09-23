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
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class SelfTrackingBackgroundTaskService {

    private lateinit var host: BackgroundTaskHost

    // In-process work runs under the host's app-lifetime scope (so it is cancelled with the app),
    // dispatched on IO since that scope is Main-dispatched but this work is blocking (uploads, hashing).
    protected val scope: CoroutineScope get() = host.processScope + Dispatchers.IO

    private val mutex = Mutex()
    private val jobs = mutableMapOf<TaskId, Job>()

    // The tasks currently running here, with their latest progress (null when a task reports none).
    // Presence of an id => it is running; this backs [liveStatus] since the OS offers no such query.
    private val progresses = MutableStateFlow<Map<TaskId, Progress?>>(emptyMap())

    /**
     * Installs the process [BackgroundTaskHost]. Call once at launch, before any task is scheduled.
     */
    fun install(
        host: BackgroundTaskHost,
    ) {
        this.host = host
        onInstalled()
        scope.launch {
            host.backgroundTaskScheduler.taskStore.pending
                .first()
                .forEach { scheduleOrLaunch(it.id) }
        }
    }

    protected open fun onInstalled() = Unit

    open suspend fun scheduleOrLaunch(
        id: TaskId,
    ) = launchInProcess(id)

    fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> =
        progresses
            .map { current ->
                if (id in current) TaskStatus.Running(current[id]) else null
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

    protected fun launchInProcess(
        id: TaskId,
    ) {
        scope.launch { runTracked(id) }
    }

    /**
     * Runs [id] on the current coroutine, registered in [jobs], so [cancel] can reach it.
     */
    protected suspend fun runTracked(
        id: TaskId,
    ): Result<Unit> {
        val job = currentCoroutineContext().job
        val started = mutex.withLock {
            if (jobs.containsKey(id)) false
            else true.also { jobs[id] = job }
        }
        if (!started) return Result.success(Unit)
        progresses.update { it + (id to null) }
        return try {
            host.backgroundTaskRunner.run(
                id = id,
                onProgress = { progress -> progresses.update { it + (id to progress) } },
            )
        } finally {
            withContext(NonCancellable) {
                mutex.withLock { if (jobs[id] === job) jobs.remove(id) }
                progresses.update { it - id }
            }
        }
    }
}
