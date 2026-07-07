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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [BackgroundTaskScheduler] that never runs anything — a placeholder for targets whose real
 * implementation has not been wired yet (currently Android and iOS).
 */
class NoOpBackgroundTaskScheduler(
    taskStore: TaskStore,
    httpClient: HttpClient,
    fileManager: FileManager,
) : BackgroundTaskScheduler(taskStore, httpClient, fileManager) {

    override suspend fun schedule(
        task: Task,
    ) = Unit

    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> = flowOf(null)

    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean = false
}
