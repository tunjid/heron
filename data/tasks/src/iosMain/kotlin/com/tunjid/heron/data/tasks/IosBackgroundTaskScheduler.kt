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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.encodeUtf8
import platform.BackgroundTasks.BGContinuedProcessingTask
import platform.BackgroundTasks.BGContinuedProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSProcessInfo

fun createBackgroundTaskScheduler(
    taskStore: TaskStore,
): BackgroundTaskScheduler = IosBackgroundTaskScheduler(taskStore)

internal class IosBackgroundTaskScheduler(
    taskStore: TaskStore,
) : BackgroundTaskScheduler(taskStore) {

    override suspend fun schedule(
        task: Task,
    ) = IosBackgroundTaskService.scheduleOrLaunch(task.id)

    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> = IosBackgroundTaskService.liveStatus(id)
    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean = IosBackgroundTaskService.cancel(id)
}

@OptIn(ExperimentalForeignApi::class)
object IosBackgroundTaskService : SelfTrackingBackgroundTaskService() {
    private val registrationMutex = Mutex()

    private val registeredIdentifiers = mutableSetOf<String>()

    override suspend fun scheduleOrLaunch(
        id: TaskId,
    ) {
        if (!supportsContinuedProcessing) return launchInProcess(id)
        val identifier = taskIdentifier(id)
        // The OS blocks registering a single handler for the wildcard pattern; the wildcard in
        // Info.plist only permits concrete identifiers. Register the concrete identifier once,
        // immediately before submitting a request for it.
        ensureHandlerRegistered(
            identifier = identifier,
            id = id,
        )
        val request = BGContinuedProcessingTaskRequest(
            identifier = identifier,
            title = ContinuedProcessingTitle,
            subtitle = "",
        )
        // If submission is declined (e.g. background scheduling is unavailable), fall back to
        // in-process so the task still runs.
        if (!BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)) launchInProcess(id)
    }

    private suspend fun ensureHandlerRegistered(
        identifier: String,
        id: TaskId,
    ) {
        val newlyRegistered = registrationMutex.withLock {
            registeredIdentifiers.add(identifier)
        }
        if (!newlyRegistered) return

        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = identifier,
            usingQueue = null,
        ) { task ->
            val continuedTask = task as? BGContinuedProcessingTask ?: return@registerForTaskWithIdentifier
            val job = scope.launch {
                continuedTask.setTaskCompletedWithSuccess(
                    success = runTracked(id).isSuccess,
                )
            }
            continuedTask.expirationHandler = {
                job.cancel()
            }
        }
    }

    private fun taskIdentifier(
        id: TaskId,
    ): String = IdentifierPrefix + id.value
        .encodeUtf8()
        .sha256()
        .hex()
        .substring(0, IdentifierTokenLength)

    private val supportsContinuedProcessing: Boolean
        get() = NSProcessInfo.processInfo.operatingSystemVersion.useContents {
            majorVersion >= MinimumContinuedProcessingMajorVersion
        }
}

private const val IdentifierPrefix = "com.tunjid.heron.tasks."
private const val ContinuedProcessingTitle = "Posting"
private const val MinimumContinuedProcessingMajorVersion = 26L

// 16 hex chars (64 bits) of the id's SHA-256: collision-safe for the handful of concurrent tasks.
private const val IdentifierTokenLength = 16
