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

package com.tunjid.heron.data.ml.model

import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.tasks.BackgroundTaskScheduler
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.tasks.TaskStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

class LiteRtLmManager(
    private val fileSystem: FileSystem,
    private val modelsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val backgroundTaskScheduler: BackgroundTaskScheduler,
    private val modelDownloadUrlResolver: ModelDownloadUrlResolver,
) : InferenceModelManager {

    override val models: List<InferenceModel> =
        listOf(
            InferenceModel.Gemma31B,
        )

    override fun status(
        model: InferenceModel,
    ): Flow<TaskStatus> = backgroundTaskScheduler.status(downloadTaskId(model))

    override suspend fun enqueueDownload(
        model: InferenceModel,
    ) {
        val liteRtLmModel = model.asLiteRtLmModel()
        val signedUrl = modelDownloadUrlResolver.resolve(liteRtLmModel.bucketPath)
        if (signedUrl == null) {
            logcat(LogPriority.WARN) {
                "Not enqueuing download for ${liteRtLmModel.name}: " +
                    "could not resolve a signed URL for ${liteRtLmModel.bucketPath}"
            }
            return
        }
        backgroundTaskScheduler.enqueue(
            Task.Download(
                sourceUrl = signedUrl,
                destination = modelPath(liteRtLmModel).toString(),
                sizeInBytes = liteRtLmModel.sizeInBytes,
                sha256 = liteRtLmModel.sha256,
            ),
        )
    }

    override suspend fun cancelDownload(
        model: InferenceModel,
    ) = backgroundTaskScheduler.cancel(downloadTaskId(model))

    override suspend fun delete(
        model: InferenceModel,
    ): Unit = withContext(ioDispatcher) {
        cancelDownload(model)
        fileSystem.delete(
            path = modelPath(model.asLiteRtLmModel()),
            mustExist = false,
        )
    }

    private fun downloadTaskId(
        model: InferenceModel,
    ): TaskId {
        val liteRtLmModel = model.asLiteRtLmModel()
        return Task.Download(
            sourceUrl = "",
            destination = modelPath(liteRtLmModel).toString(),
            sizeInBytes = liteRtLmModel.sizeInBytes,
            sha256 = liteRtLmModel.sha256,
        ).id
    }

    private fun modelPath(model: LiteRtLmModel): Path =
        modelsDirectory / model.modelFile
}
