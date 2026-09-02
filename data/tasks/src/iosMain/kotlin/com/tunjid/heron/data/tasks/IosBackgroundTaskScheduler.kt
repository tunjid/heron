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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid

/**
 * iOS [BackgroundTaskScheduler]. Out of band execution is not wired up yet, so [schedule] is inert
 * and [Task.Download] does nothing; what is implemented is [keepingProcessAlive], the hold an
 * in-flight upload takes so a backgrounded app is not suspended mid write.
 */
internal class IosBackgroundTaskScheduler(
    taskStore: TaskStore,
    httpClient: HttpClient,
    fileManager: FileManager,
) : BackgroundTaskScheduler(taskStore, httpClient, fileManager) {

    /**
     * iOS suspends a backgrounded app, so the hold is a background task assertion. It is best
     * effort: the system grants a finite window and reclaims it by calling the expiration handler,
     * after which the write runs unprotected until the app is suspended.
     */
    override suspend fun <T> keepingProcessAlive(
        id: TaskId,
        block: suspend () -> T,
    ): T = super.keepingProcessAlive(id) {
        val assertion = BackgroundAssertion()
        // UIApplication is main thread only, and the expiration handler is called there too, so
        // both ends of the assertion are serialized without needing an atomic.
        withContext(Dispatchers.Main) {
            assertion.identifier = UIApplication.sharedApplication
                .beginBackgroundTaskWithName(id.value) { assertion.end() }
        }
        try {
            block()
        } finally {
            withContext(NonCancellable + Dispatchers.Main) { assertion.end() }
        }
    }

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

/** iOS kills the app outright if an assertion outlives its expiration, and ending one twice is an error. */
private class BackgroundAssertion {

    var identifier: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid

    fun end() {
        if (identifier == UIBackgroundTaskInvalid) return
        UIApplication.sharedApplication.endBackgroundTask(identifier)
        identifier = UIBackgroundTaskInvalid
    }
}

fun createBackgroundTaskScheduler(
    taskStore: TaskStore,
    httpClient: HttpClient,
    fileManager: FileManager,
): BackgroundTaskScheduler = IosBackgroundTaskScheduler(
    taskStore = taskStore,
    httpClient = httpClient,
    fileManager = fileManager,
)
