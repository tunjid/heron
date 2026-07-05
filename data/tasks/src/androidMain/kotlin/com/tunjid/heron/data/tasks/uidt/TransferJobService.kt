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

import android.app.job.JobParameters
import android.app.job.JobService
import androidx.annotation.RequiresApi
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TransferNotifications
import com.tunjid.heron.data.tasks.runTransfer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The user-initiated data-transfer job (API 34+). `onStartJob` returns `true` and keeps running on a
 * coroutine; a visible notification is attached via [setNotification] (required for UIDT jobs) and
 * the outcome is written to the [com.tunjid.heron.data.tasks.TaskStore] by [runTransfer].
 */
@RequiresApi(34)
class TransferJobService : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val jobs = mutableMapOf<Int, Job>()

    override fun onStartJob(
        params: JobParameters,
    ): Boolean {
        val id = TaskId(params.extras.getString(KeyTaskId) ?: return false)
        jobs[params.jobId] = scope.launch {
            val outcome = applicationContext.runTransfer(
                id = id,
            ) { task, progress ->
                setNotification(
                    /* params = */
                    params,
                    /* notificationId = */
                    TransferNotifications.notificationId(id),
                    /* notification = */
                    TransferNotifications.build(applicationContext, task.destination, progress),
                    /* jobEndNotificationPolicy = */
                    JOB_END_NOTIFICATION_POLICY_REMOVE,
                )
            }
            jobs.remove(params.jobId)
            jobFinished(
                params,
                /* wantsReschedule = */
                outcome.isFailure,
            )
        }
        return true
    }

    override fun onStopJob(
        params: JobParameters,
    ): Boolean {
        jobs.remove(params.jobId)?.cancel()
        // The transfer can resume from its `.part` file, so let the system reschedule if it wants.
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val KeyTaskId = "taskId"
    }
}
