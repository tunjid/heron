package com.tunjid.heron.data.utilities.inference

import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.files.FileManager
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import okio.Path

internal class LiteRtLmManager(
    private val fileManager: FileManager,
    private val modelsDirectory: Path,
    private val ioDispatcher: CoroutineDispatcher,
    private val backgroundTaskScheduler: BackgroundTaskScheduler,
    private val networkService: NetworkService,
) : InferenceModelManager {

    override val models: List<InferenceModel> =
        listOf(
            InferenceModel.Gemma31B,
            InferenceModel.Gemma4E2B,
            InferenceModel.Gemma4E4B,
        )

    override fun status(
        model: InferenceModel,
    ): Flow<ModelStatus> = combine(
        backgroundTaskScheduler.status(downloadTaskId(model)),
        // A deletion (or a completed download's move) changes no task state, so react to the file
        // mutation directly; [onStart] seeds [combine]'s first emission.
        fileManager.fileMutations
            .filter { it == modelPath(model) }
            .onStart { emit(modelPath(model)) },
    ) { taskStatus, _ ->
        if (fileManager.exists(modelPath(model))) ModelStatus.Downloaded(
            LoadedModel(
                model = model,
                path = modelPath(model),
            ),
        )
        else ModelStatus.Pending(
            taskStatus = taskStatus,
        )
    }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

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
        fileManager.delete(modelPath(model))
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
