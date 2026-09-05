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

package com.tunjid.heron.data.tasks.workmanager

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.tunjid.heron.data.tasks.KeyCompletedBytes
import com.tunjid.heron.data.tasks.KeyTotalBytes
import com.tunjid.heron.data.tasks.Progress
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TransferNotice
import com.tunjid.heron.data.tasks.TransferNotifications
import com.tunjid.heron.data.tasks.TransferNotifications.progressNotification
import com.tunjid.heron.data.tasks.pendingNotice
import com.tunjid.heron.data.tasks.runTransfer

/**
 * WorkManager fallback (API < 34) that runs the task as a foreground service, so it survives the app
 * being backgrounded. The task id is passed in the input data; everything else is read from the
 * [com.tunjid.heron.data.tasks.TaskStore] in [runTransfer].
 */
internal class TransferWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val id = TaskId(inputData.getString(KeyTaskId) ?: return Result.failure())
        val outcome = applicationContext.runTransfer(
            id = id,
        ) { notice ->
            setForeground(
                foregroundInfo(
                    id = id,
                    notice = notice,
                ),
            )
            notice.progress?.let { setProgress(progressData(it)) }
        }
        return if (outcome.isSuccess) Result.success() else Result.failure()
    }

    // Required for expedited work: shown while WorkManager runs the request as a foreground service.
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val id = TaskId(inputData.getString(KeyTaskId).orEmpty())
        return foregroundInfo(
            id = id,
            notice = applicationContext.pendingNotice(id),
        )
    }

    private fun foregroundInfo(
        id: TaskId,
        notice: TransferNotice,
    ): ForegroundInfo {
        val notification = applicationContext.progressNotification(notice)
        val notificationId = TransferNotifications.notificationId(id)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        ) else ForegroundInfo(
            notificationId,
            notification,
        )
    }

    private fun progressData(
        progress: Progress,
    ): Data = Data.Builder()
        .putLong(KeyCompletedBytes, progress.completedBytes)
        .putLong(KeyTotalBytes, progress.totalBytes)
        .build()

    companion object {
        const val KeyTaskId = "taskId"
    }
}
