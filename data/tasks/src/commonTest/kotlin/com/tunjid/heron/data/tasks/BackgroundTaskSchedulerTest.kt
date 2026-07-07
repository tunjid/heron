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

import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.files.createFileManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.SYSTEM

class BackgroundTaskSchedulerTest {

    private val download = Task.Download(
        sourceUrl = "https://example.com/model.bin",
        destination = File.System("/tmp/heron/models/model.bin"),
        sizeInBytes = 100L,
    )

    private val httpClient = HttpClient(MockEngine { respondOk() })

    @Test
    fun status_is_not_found_when_unknown() = runTest {
        assertEquals(TaskStatus.NotFound, scheduler().status(download.id).first())
    }

    @Test
    fun status_is_created_when_pending_and_not_running() = runTest {
        val store = FakeTaskStore()
        val scheduler = scheduler(store = store)
        store.add(download)
        assertEquals(TaskStatus.Created, scheduler.status(download.id).first())
    }

    @Test
    fun status_reflects_the_platform_running_signal() = runTest {
        val store = FakeTaskStore()
        val live = MutableStateFlow<TaskStatus.Running?>(null)
        val scheduler = scheduler(store = store, live = live)
        store.add(download)
        val progress = Progress(completedBytes = 50L, totalBytes = 100L)
        live.value = TaskStatus.Running(progress)
        assertEquals(TaskStatus.Running(progress), scheduler.status(download.id).first())
    }

    @Test
    fun status_is_failed_when_recorded_failed() = runTest {
        val store = FakeTaskStore()
        val scheduler = scheduler(store = store)
        store.add(download)
        store.markFailed(download.id, reason = "boom")
        assertEquals(TaskStatus.Failed("boom"), scheduler.status(download.id).first())
    }

    @Test
    fun enqueue_persists_and_schedules() = runTest {
        val store = FakeTaskStore()
        val scheduler = scheduler(store = store)
        scheduler.enqueue(download)
        assertEquals(
            expected = listOf(element = download),
            actual = store.pending.first(),
        )
        assertTrue(scheduler.scheduled.contains(download))
    }

    @Test
    fun enqueue_of_a_duplicate_is_not_scheduled_again() = runTest {
        val scheduler = scheduler()
        scheduler.enqueue(download)
        scheduler.enqueue(download)
        assertEquals(
            expected = listOf<Task>(element = download),
            actual = scheduler.scheduled,
        )
    }

    @Test
    fun cancel_forgets_the_task_when_a_job_was_cancelled() = runTest {
        val store = FakeTaskStore()
        val scheduler = scheduler(store = store, cancelResult = true)
        scheduler.enqueue(download)
        scheduler.cancel(download.id)
        assertEquals(
            expected = emptyList(),
            actual = store.pending.first(),
        )
    }

    @Test
    fun cancel_clears_the_task_when_nothing_was_scheduled() = runTest {
        val store = FakeTaskStore()
        val scheduler = scheduler(store = store, cancelResult = false)
        scheduler.enqueue(download)
        scheduler.cancel(download.id)
        assertEquals(
            expected = emptyList(),
            actual = store.pending.first(),
        )
    }

    private fun scheduler(
        store: TaskStore = FakeTaskStore(),
        live: MutableStateFlow<TaskStatus.Running?> = MutableStateFlow(null),
        cancelResult: Boolean = true,
    ): TestBackgroundTaskScheduler = TestBackgroundTaskScheduler(
        taskStore = store,
        httpClient = httpClient,
        live = live,
        cancelResult = cancelResult,
    )
}

private class TestBackgroundTaskScheduler(
    taskStore: TaskStore,
    httpClient: HttpClient,
    private val live: MutableStateFlow<TaskStatus.Running?>,
    private val cancelResult: Boolean,
) : BackgroundTaskScheduler(
    taskStore = taskStore,
    httpClient = httpClient,
    fileManager = createFileManager(
        fileSystem = FileSystem.SYSTEM,
    ),
) {

    val scheduled = mutableListOf<Task>()

    override suspend fun schedule(
        task: Task,
    ) {
        scheduled += task
    }

    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> = live

    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean = cancelResult
}

private class FakeTaskStore : TaskStore {
    private val pendingState = MutableStateFlow<List<Task>>(emptyList())
    private val failedState = MutableStateFlow<List<FailedTask>>(emptyList())

    override val pending: Flow<List<Task>> = pendingState
    override val failed: Flow<List<FailedTask>> = failedState

    override suspend fun add(
        task: Task,
    ): Boolean {
        if (pendingState.value.any { it.id == task.id }) return false
        pendingState.value += task
        return true
    }

    override suspend fun remove(
        id: TaskId,
    ) {
        pendingState.value = pendingState.value.filterNot { it.id == id }
        failedState.value = failedState.value.filterNot { it.task.id == id }
    }

    override suspend fun markFailed(
        id: TaskId,
        reason: String?,
    ) {
        val task = pendingState.value.firstOrNull { it.id == id } ?: return
        pendingState.value = pendingState.value.filterNot { it.id == id }
        failedState.value = failedState.value + FailedTask(task = task, reason = reason)
    }
}
