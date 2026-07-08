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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tunjid.heron.data.tasks.KeyCompletedBytes
import com.tunjid.heron.data.tasks.KeyTotalBytes
import com.tunjid.heron.data.tasks.Progress
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TaskStatus
import com.tunjid.heron.data.tasks.TransferDelegate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class WorkManagerTransferDelegate(
    context: Context,
) : TransferDelegate {

    private val workManager = WorkManager.getInstance(context)

    override suspend fun schedule(
        task: Task.Download,
    ) {
        val request = OneTimeWorkRequestBuilder<TransferWorker>()
            .setInputData(
                Data.Builder()
                    .putString(TransferWorker.KeyTaskId, task.id.value)
                    .build(),
            )
            // Run promptly; falls back to regular work (not dropped) if the expedited quota is spent.
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName = task.id.value,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = request,
        )
    }

    override fun liveStatus(
        id: TaskId,
    ): Flow<TaskStatus.Running?> =
        workManager.getWorkInfosForUniqueWorkFlow(id.value)
            .map { infos ->
                infos.firstOrNull()
                    ?.takeIf { it.state == WorkInfo.State.RUNNING }
                    ?.progress
                    ?.let { data ->
                        val total = data.getLong(
                            key = KeyTotalBytes,
                            defaultValue = 0L,
                        )
                        TaskStatus.Running(
                            progress =
                            if (total > 0L) Progress(
                                completedBytes = data.getLong(
                                    key = KeyCompletedBytes,
                                    defaultValue = 0L,
                                ),
                                totalBytes = total,
                            )
                            else null,
                        )
                    }
            }
            .distinctUntilChanged()

    override suspend fun cancelScheduled(
        id: TaskId,
    ): Boolean {
        val running = workManager.getWorkInfosForUniqueWorkFlow(id.value)
            .first()
            .any { !it.state.isFinished }
        workManager.cancelUniqueWork(id.value)
        return running
    }
}
