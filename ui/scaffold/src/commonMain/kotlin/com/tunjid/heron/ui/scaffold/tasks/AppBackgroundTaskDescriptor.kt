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

package com.tunjid.heron.ui.scaffold.tasks

import com.tunjid.heron.data.tasks.BackgroundTaskDescriptor
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskDescription
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.timeline.utilities.describe
import com.tunjid.heron.timeline.utilities.mediaSummaryMessage
import com.tunjid.heron.ui.text.message
import dev.zacsweers.metro.Inject
import heron.ui.scaffold.generated.resources.Res
import heron.ui.scaffold.generated.resources.background_task_downloading
import heron.ui.scaffold.generated.resources.background_task_syncing
import heron.ui.scaffold.generated.resources.notification_channel_background_tasks
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.getString

/**
 * Describes background tasks with the same vocabulary the app uses in-app: writes read like their
 * entries on the tasks screen, with media counts in place of post text so drafts stay off the lock
 * screen.
 */
@Inject
class AppBackgroundTaskDescriptor(
    private val writeQueue: WriteQueue,
) : BackgroundTaskDescriptor {

    override suspend fun describe(
        task: Task,
    ): TaskDescription = when (task) {
        is Task.Write -> describeWrite(task)
        is Task.Download -> TaskDescription(
            title = getString(Res.string.background_task_downloading),
            subtitle = task.destination.relativePath.substringAfterLast('/'),
            destination = null,
        )
    }

    override suspend fun channelName(): String =
        getString(Res.string.notification_channel_background_tasks)

    private suspend fun describeWrite(
        task: Task.Write,
    ): TaskDescription {
        val description = try {
            writeQueue.backgroundWrite(task)?.describe()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
        return TaskDescription(
            title = description?.title?.message()
                ?: getString(Res.string.background_task_syncing),
            subtitle = description?.mediaSummaryMessage(),
            destination = TasksRoute,
        )
    }
}

// Matches TasksRoutePattern in :feature:tasks.
private const val TasksRoute = "/tasks"
