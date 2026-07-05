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
import android.os.Build
import com.tunjid.heron.data.tasks.TransferNotifications.ensureChannel
import com.tunjid.heron.data.tasks.uidt.UidtTransferDelegate
import com.tunjid.heron.data.tasks.workmanager.WorkManagerTransferDelegate
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

/**
 * Android [BackgroundTaskScheduler]. A single scheduler that selects a [TransferDelegate] by API
 * level: a user-initiated data-transfer `JobService` (API 34+, preferred) or a WorkManager
 * foreground-service worker (fallback). Both run in the app's process and reach this scheduler — and
 * thus the shared `httpClient` and [taskStore] — via `context.backgroundTaskScheduler`.
 */
class AndroidBackgroundTaskScheduler(
    context: Context,
    taskStore: TaskStore,
    httpClient: HttpClient,
) : BackgroundTaskScheduler(taskStore, httpClient) {

    init {
        context.ensureChannel()
    }

    private val delegate: TransferDelegate =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) UidtTransferDelegate(
            context,
        )
        else WorkManagerTransferDelegate(context)

    override suspend fun schedule(
        task: Task,
    ) {
        if (task is Task.Download) delegate.schedule(task)
    }

    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> = delegate.liveStatus(id)

    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean = delegate.cancelScheduled(id)
}

fun createBackgroundTaskScheduler(
    context: Context,
    taskStore: TaskStore,
    httpClient: HttpClient,
): BackgroundTaskScheduler = AndroidBackgroundTaskScheduler(
    context = context.applicationContext,
    taskStore = taskStore,
    httpClient = httpClient,
)
