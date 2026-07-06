package com.tunjid.heron.data.utilities.inference

import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.network.NetworkService
import com.tunjid.heron.data.tasks.BackgroundTaskScheduler
import com.tunjid.heron.data.tasks.Task
import com.tunjid.heron.data.tasks.TaskId
import com.tunjid.heron.data.utilities.mapCatchingUnlessCancelled
import com.tunjid.heron.data.utilities.toOutcome
import dev.tunji.heron.GetModelUrlQueryParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

internal class LiteRtLmManager(
    private val fileSystem: FileSystem,
    private val modelsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val backgroundTaskScheduler: BackgroundTaskScheduler,
    private val networkService: NetworkService,
) : InferenceModelManager {

    override val models: List<InferenceModel> =
        listOf(
            InferenceModel.Gemma31B,
        )

    override fun status(
        model: InferenceModel,
    ): Flow<ModelStatus> = backgroundTaskScheduler
        .status(downloadTaskId(model))
        .map {
            if (fileSystem.exists(modelPath(model))) ModelStatus.Downloaded(
                LoadedModel(
                    model = model,
                    path = modelPath(model),
                ),
            )
            else ModelStatus.Pending(
                taskStatus = it,
            )
        }

    override suspend fun enqueueDownload(
        model: InferenceModel,
    ): Outcome = networkService.runCatchingWithMonitoredNetworkRetry {
        getModelUrl(
            GetModelUrlQueryParams(
                path = model.fileName,
            ),
        )
    }
        .mapCatchingUnlessCancelled {
            backgroundTaskScheduler.enqueue(
                Task.Download(
                    sourceUrl = it.url.uri,
                    destination = modelPath(model).toString(),
                    sizeInBytes = model.sizeInBytes,
                    sha256 = model.sha256,
                ),
            )
        }
        .toOutcome()

    override suspend fun cancelDownload(
        model: InferenceModel,
    ) = backgroundTaskScheduler.cancel(downloadTaskId(model))

    override suspend fun delete(
        model: InferenceModel,
    ): Unit = withContext(ioDispatcher) {
        cancelDownload(model)
        fileSystem.delete(
            path = modelPath(model),
            mustExist = false,
        )
    }

    private fun downloadTaskId(
        model: InferenceModel,
    ): TaskId = Task.Download(
        sourceUrl = "",
        destination = modelPath(model).toString(),
        sizeInBytes = model.sizeInBytes,
        sha256 = model.sha256,
    ).id

    private fun modelPath(model: InferenceModel): Path =
        modelsDirectory / model.fileName
}
