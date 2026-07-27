package com.tunjid.heron.data.utilities.inference

import com.tunjid.heron.data.core.utilities.File
import com.tunjid.heron.data.core.utilities.Outcome
import com.tunjid.heron.data.files.FileManager
import com.tunjid.heron.data.files.asSystemFile
import com.tunjid.heron.data.files.path
import com.tunjid.heron.data.ml.engine.InferenceSource
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

internal class LiteRtLmManager(
    private val fileManager: FileManager,
    private val modelsDirectory: File.System,
    private val ioDispatcher: CoroutineDispatcher,
    private val backgroundTaskScheduler: BackgroundTaskScheduler,
    private val networkService: NetworkService,
) : InferenceModelManager {

    override val source: Flow<InferenceSource> =
        flowOf(InferenceSource.External)

    override val models: List<InferenceModel> =
        listOf(
            InferenceModel.Gemma31B,
            InferenceModel.Gemma4E2B,
            InferenceModel.Gemma4E4B,
        )

    override fun status(
        model: InferenceModel,
    ): Flow<ModelStatus> {
        val downloadable = model.downloadable()
        return combine(
            backgroundTaskScheduler.status(downloadTaskId(downloadable)),
            // A deletion (or a completed download's move) changes no task state, so react to the file
            // mutation directly; [onStart] seeds [combine]'s first emission.
            fileManager.fileMutations
                .filter { it == modelFile(downloadable) }
                .onStart { emit(modelFile(downloadable)) },
        ) { taskStatus, _ ->
            if (fileManager.exists(modelFile(downloadable))) ModelStatus.Available(
                LoadedModel.FileBacked(
                    model = downloadable,
                    file = modelFile(downloadable),
                ),
            )
            else ModelStatus.Pending(
                taskStatus = taskStatus,
            )
        }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun enqueueDownload(
        model: InferenceModel.External,
    ): Outcome {
        val downloadable = model.downloadable()
        return networkService.runCatchingWithMonitoredNetworkRetry {
            getModelUrl(
                GetModelUrlQueryParams(
                    path = downloadable.fileName,
                ),
            )
        }
            .mapCatchingUnlessCancelled {
                backgroundTaskScheduler.enqueue(
                    Task.Download(
                        sourceUrl = it.url.uri,
                        destination = modelFile(downloadable),
                        sizeInBytes = downloadable.sizeInBytes,
                        sha256 = downloadable.sha256,
                    ),
                )
            }
            .toOutcome()
    }

    override suspend fun cancelDownload(
        model: InferenceModel.External,
    ) = backgroundTaskScheduler.cancel(downloadTaskId(model.downloadable()))

    override suspend fun delete(
        model: InferenceModel.External,
    ): Unit = withContext(ioDispatcher) {
        cancelDownload(model)
        fileManager.delete(modelFile(model.downloadable()))
    }

    private fun downloadTaskId(
        model: InferenceModel.External,
    ): TaskId = Task.Download(
        sourceUrl = "",
        destination = modelFile(model),
        sizeInBytes = model.sizeInBytes,
        sha256 = model.sha256,
    ).id

    private fun modelFile(model: InferenceModel.External): File.System =
        (modelsDirectory.path / model.fileName).asSystemFile()

    // This manager only serves downloadable models (the Gemma catalog above).
    private fun InferenceModel.downloadable(): InferenceModel.External =
        this as? InferenceModel.External
            ?: error("LiteRtLmManager only handles downloadable models, got $this")
}
