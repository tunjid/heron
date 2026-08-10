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
package com.tunjid.heron.sheets.inference

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.Cursor
import com.tunjid.heron.data.core.models.CursorQuery
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.types.PostUri
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.engine.GenerationParams
import com.tunjid.heron.data.ml.engine.InferenceEngine
import com.tunjid.heron.data.ml.model.InferenceModel
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.ml.model.PlatformUnavailableReason
import com.tunjid.heron.data.repository.ProfileRepository
import com.tunjid.heron.data.repository.TimelineQuery
import com.tunjid.heron.data.repository.TimelineRepository
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.mutator.coroutines.launchedCollectLatest
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.inference_error_failed
import heron.ui.timeline.generated.resources.inference_error_model_not_loaded
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

@Stable
interface InferenceStateHolder :
    SheetStateHolder,
    ActionSuspendingStateMutator<InferenceAction, InferenceState>

@AssistedFactory
fun interface InferenceViewModelInitializer {
    fun invoke(
        scope: CoroutineScope,
    ): InferenceViewModel
}

class InferenceViewModel(
    mutator: ActionSuspendingStateMutator<InferenceAction, InferenceState>,
    scope: CoroutineScope,
) : ViewModel(viewModelScope = scope),
    InferenceStateHolder,
    ActionSuspendingStateMutator<InferenceAction, InferenceState> by mutator {

    @AssistedInject
    constructor(
        inferenceEngine: InferenceEngine,
        inferenceModelManager: InferenceModelManager,
        userDataRepository: UserDataRepository,
        profileRepository: ProfileRepository,
        timelineRepository: TimelineRepository,
        navActions: (NavigationMutation) -> Unit,
        @Assisted scope: CoroutineScope,
    ) : this(
        mutator = scope.actionSuspendingStateMutator(
            state = InferenceState.Immutable().toSnapshotMutable(),
            started = SharingStarted.WhileSubscribed(SheetWhileSubscribed),
            producer = { state, actions ->
                launchEngineStateMutations(
                    state = state,
                    inferenceEngine = inferenceEngine,
                )
                actions.launchMutationsIn(
                    productionScope = this,
                    keySelector = InferenceAction::key,
                ) {
                    when (val action = type()) {
                        is InferenceAction.Translate -> action.flow.launchTranslationMutations(
                            state = state,
                            inferenceEngine = inferenceEngine,
                            inferenceModelManager = inferenceModelManager,
                            userDataRepository = userDataRepository,
                        )
                        is InferenceAction.Vibe -> action.flow.launchVibeMutations(
                            state = state,
                            inferenceEngine = inferenceEngine,
                            inferenceModelManager = inferenceModelManager,
                            userDataRepository = userDataRepository,
                            profileRepository = profileRepository,
                            timelineRepository = timelineRepository,
                        )
                        is InferenceAction.Tea -> action.flow.launchTeaMutations(
                            state = state,
                            inferenceEngine = inferenceEngine,
                            inferenceModelManager = inferenceModelManager,
                            userDataRepository = userDataRepository,
                            timelineRepository = timelineRepository,
                        )
                        is InferenceAction.Navigate.To -> action.flow.collect { navAction ->
                            navActions(navAction.navigationMutation)
                        }
                    }
                }
            },
        ),
        scope = scope,
    )
}

context(productionScope: CoroutineScope)
private fun launchEngineStateMutations(
    state: InferenceState.SnapshotMutable,
    inferenceEngine: InferenceEngine,
) = inferenceEngine.state.launchedCollect {
    state.engineState = it
}

context(productionScope: CoroutineScope)
private fun Flow<InferenceAction.Translate>.launchTranslationMutations(
    state: InferenceState.SnapshotMutable,
    inferenceEngine: InferenceEngine,
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
) = launchedCollectLatest { action ->
    state.kind = InferenceKind.Translation
    inferenceEngine.outcomes(
        inferenceModelManager = inferenceModelManager,
        userDataRepository = userDataRepository,
        // Near-greedy decoding: translation is a constrained task, so a low temperature keeps
        // the output faithful and free of the preamble and format drift that higher
        // temperatures invite on small on-device models.
        params = GenerationParams(temperature = 0.2f),
        prompt = translationPrompt(
            text = action.post.record?.text.orEmpty(),
            sourceLanguageTag = action.sourceLanguage,
            targetLanguageTag = action.targetLanguage,
        ),
        transform = String::unwrapTranslation,
    ).collect { outcome ->
        state.translationOutcome = outcome
    }
}

context(productionScope: CoroutineScope)
private fun Flow<InferenceAction.Vibe>.launchVibeMutations(
    state: InferenceState.SnapshotMutable,
    inferenceEngine: InferenceEngine,
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
    profileRepository: ProfileRepository,
    timelineRepository: TimelineRepository,
) = distinctUntilChanged()
    .launchedCollectLatest { action ->
        state.kind = InferenceKind.Vibe
        // A new profile invalidates the lenses cached for the previous one.
        if (state.vibeProfileId != action.profileId) {
            state.vibeProfileId = action.profileId
            state.postsOutcome = null
            state.repliesOutcome = null
        }
        // A lens that already succeeded for this profile never needs regenerating.
        if (state.vibeOutcome(action.type) is InferenceOutcome.Success) {
            return@launchedCollectLatest
        }
        state.setVibeOutcome(
            type = action.type,
            outcome = InferenceOutcome.Loading(),
        )

        val profile = withTimeoutOrNull(VibeFetchTimeout) {
            profileRepository.profile(action.profileId).first()
        }
        if (profile == null) {
            state.setVibeOutcome(
                type = action.type,
                outcome = InferenceOutcome.Error(
                    memo = Memo.Resource(Res.string.inference_error_failed),
                ),
            )
            return@launchedCollectLatest
        }
        inferenceEngine.outcomes(
            inferenceModelManager = inferenceModelManager,
            userDataRepository = userDataRepository,
            prompt = vibePrompt(
                items = timelineRepository.recentTimelineItems(
                    profileId = action.profileId,
                    type = action.type,
                ),
                profile = profile,
                type = action.type,
            ),
            transform = String::trim,
        ).collect { outcome ->
            state.setVibeOutcome(
                type = action.type,
                outcome = outcome,
            )
        }
    }

context(productionScope: CoroutineScope)
private fun Flow<InferenceAction.Tea>.launchTeaMutations(
    state: InferenceState.SnapshotMutable,
    inferenceEngine: InferenceEngine,
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
    timelineRepository: TimelineRepository,
) = distinctUntilChanged()
    .launchedCollectLatest { action ->
        state.kind = InferenceKind.Tea
        val anchorPostUri = action.post.uri
        // A new thread invalidates the tea cached for the previous one.
        if (state.teaPostUri != anchorPostUri) {
            state.teaPostUri = anchorPostUri
            state.teaOutcome = null
        }
        // A thread whose tea already succeeded never needs regenerating.
        if (state.teaOutcome is InferenceOutcome.Success) {
            return@launchedCollectLatest
        }
        state.teaOutcome = InferenceOutcome.Loading()

        val items = timelineRepository.quoteThreadItems(
            postUri = anchorPostUri,
        )
        if (items.isEmpty()) {
            state.teaOutcome = InferenceOutcome.Error(
                memo = Memo.Resource(Res.string.inference_error_failed),
            )
            return@launchedCollectLatest
        }
        inferenceEngine.outcomes(
            inferenceModelManager = inferenceModelManager,
            userDataRepository = userDataRepository,
            prompt = teaPrompt(items = items),
            transform = String::trim,
        ).collect { outcome ->
            state.teaOutcome = outcome
        }
    }

/**
 * Streams [InferenceOutcome]s for a single [prompt]: an initial [InferenceOutcome.Loading] whose
 * [text][InferenceOutcome.text] grows with each streamed token (post-processed by [transform]),
 * then a terminal [InferenceOutcome.Success]. Emits [InferenceOutcome.Error] if no model is loaded
 * or generation fails.
 */
private fun InferenceEngine.outcomes(
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
    prompt: String,
    params: GenerationParams = GenerationParams(),
    transform: (String) -> String = { it },
): Flow<InferenceOutcome> = flow {
    emit(InferenceOutcome.Loading())
    if (state.first() !is EngineState.Ready) {
        // Opportunistically load the selected default model; prompt the user when there is none, or
        // explain why a present-but-not-ready platform model can't run yet.
        when (
            val resolution = resolveDefaultModel(
                inferenceModelManager = inferenceModelManager,
                userDataRepository = userDataRepository,
            )
        ) {
            // Loading an already loaded model is idempotent across engine implementations.
            is DefaultModelResolution.Loadable -> load(resolution.model)
            is DefaultModelResolution.Unavailable -> {
                emit(InferenceOutcome.Unavailable(resolution.reason))
                return@flow
            }
            DefaultModelResolution.None -> {
                emit(InferenceOutcome.NoModel)
                return@flow
            }
        }
    }
    if (state.first() !is EngineState.Ready) {
        emit(
            InferenceOutcome.Error(
                memo = Memo.Resource(Res.string.inference_error_model_not_loaded),
            ),
        )
        return@flow
    }
    val buffer = StringBuilder()
    generate(
        prompt = prompt,
        params = params,
    ).collect { token ->
        buffer.append(token)
        emit(
            InferenceOutcome.Loading(
                text = buffer.transformedOrPlain(transform),
            ),
        )
    }
    emit(
        InferenceOutcome.Success(
            text = buffer.transformedOrPlain(transform),
        ),
    )
}.catch {
    emit(
        InferenceOutcome.Error(
            memo = Memo.Resource(Res.string.inference_error_failed),
        ),
    )
}

private sealed interface DefaultModelResolution {
    data class Loadable(
        val model: LoadedModel,
    ) : DefaultModelResolution

    data class Unavailable(
        val reason: PlatformUnavailableReason,
    ) : DefaultModelResolution

    data object None : DefaultModelResolution
}

private suspend fun resolveDefaultModel(
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
): DefaultModelResolution {
    // A platform system model needs no download or default-name selection; use it when available,
    // and otherwise surface why it isn't (rather than treating it as "no model").
    inferenceModelManager.models
        .firstOrNull { it is InferenceModel.Platform }
        ?.let { platformModel ->
            return when (val status = inferenceModelManager.status(platformModel).first()) {
                is ModelStatus.Available -> DefaultModelResolution.Loadable(status.loadedModel)
                is ModelStatus.Unavailable -> DefaultModelResolution.Unavailable(status.reason)
                else -> DefaultModelResolution.None
            }
        }
    val defaultModelName = userDataRepository.preferences.first().local.defaultModelName
        ?: return DefaultModelResolution.None
    val model = inferenceModelManager.models.firstOrNull { it.name == defaultModelName }
        ?: return DefaultModelResolution.None
    return when (val status = inferenceModelManager.status(model).first()) {
        is ModelStatus.Available -> DefaultModelResolution.Loadable(status.loadedModel)
        else -> DefaultModelResolution.None
    }
}

private suspend fun TimelineRepository.recentTimelineItems(
    profileId: ProfileId,
    type: Timeline.Profile.Type,
): List<TimelineItem> =
    withTimeoutOrNull(VibeFetchTimeout) {
        timelineItems(
            query = TimelineQuery(
                data = CursorQuery.defaultStartData(limit = VibeSampleLimit),
                source = Timeline.Source.Profile(
                    profileId = profileId,
                    type = type,
                ),
            ),
            cursor = Cursor.Initial(),
        )
            .mapNotNull { cursorList ->
                cursorList.items.takeIf(List<TimelineItem>::isNotEmpty)
            }
            .first()
    }
        .orEmpty()

private suspend fun TimelineRepository.quoteThreadItems(
    postUri: PostUri,
): List<TimelineItem> =
    withTimeoutOrNull(TeaFetchTimeout) {
        postQuoteThread(postUri = postUri)
            .mapNotNull { items ->
                items.takeIf(List<TimelineItem>::isNotEmpty)
            }
            .first()
    }
        .orEmpty()

private inline fun StringBuilder.transformedOrPlain(
    transform: (String) -> String,
): String {
    val string = toString()
    return transform(string).ifEmpty { string.trim() }
}

/**
 * Strips wrappers a model may add around a translation despite instructions, surrounding code
 * fences or quotes, so the sheet always renders plain text. Only removes a wrapper present on
 * both ends, leaving quotes that belong to the text itself untouched.
 */
private fun String.unwrapTranslation(): String =
    trim()
        .removeSurrounding("```").trim()
        .removeSurrounding("\"")
        .removeSurrounding("“", "”")
        .trim()

private const val VibeSampleLimit = 15L
private val VibeFetchTimeout = 20.seconds
private val TeaFetchTimeout = 20.seconds
