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

package com.tunjid.heron.data.tasks.uidt

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.RequiresApi
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TaskStatus
import com.tunjid.heron.data.tasks.TransferDelegate
import com.tunjid.heron.data.tasks.TransferNotifications.notificationProgress
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

@RequiresApi(34)
internal class UidtTransferDelegate(
    private val context: Context,
) : TransferDelegate {

    private val jobScheduler = context.getSystemService(JobScheduler::class.java)

    override suspend fun schedule(
        task: Task.Download,
    ) {
        val jobInfo = JobInfo.Builder(
            task.id.jobId(),
            ComponentName(context, TransferJobService::class.java),
        )
            .setUserInitiated(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setEstimatedNetworkBytes(
                task.sizeInBytes,
                0L,
            )
            .setExtras(
                PersistableBundle().apply { putString(TransferJobService.KeyTaskId, task.id.value) },
            )
            .build()
        jobScheduler.schedule(jobInfo)
    }

    // JobScheduler has no live callback, so poll for the job's presence: `getPendingJob` returns the
    // job until it calls `jobFinished`, so a non-null result means it is still running. Progress comes
    // from the job's own notification.
    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> = flow {
        while (true) {
            val hasPendingJob = jobScheduler.getPendingJob(id.jobId()) != null
            emit(
                if (hasPendingJob) TaskStatus.Running(context.notificationProgress(id))
                else null,
            )
            delay(PollInterval)
        }
    }
        .distinctUntilChanged()

    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean {
        val running = jobScheduler.getPendingJob(id.jobId()) != null
        jobScheduler.cancel(id.jobId())
        return running
    }
}

private fun TaskId.jobId(): Int = value.hashCode()

private val PollInterval = 1.seconds
