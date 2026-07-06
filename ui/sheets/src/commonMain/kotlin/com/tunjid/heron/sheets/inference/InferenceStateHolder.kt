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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.engine.GenerationParams
import com.tunjid.heron.data.ml.engine.InferenceEngine
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
import com.tunjid.heron.data.repository.UserDataRepository
import com.tunjid.heron.sheets.utilities.SheetWhileSubscribed
import com.tunjid.heron.ui.scaffold.navigation.NavigationAction
import com.tunjid.heron.ui.scaffold.navigation.NavigationMutation
import com.tunjid.heron.ui.stateproduction.SheetStateHolder
import com.tunjid.heron.ui.text.Memo
import com.tunjid.mutator.coroutines.ActionSuspendingStateMutator
import com.tunjid.mutator.coroutines.actionSuspendingStateMutator
import com.tunjid.mutator.coroutines.launchMutationsIn
import com.tunjid.mutator.coroutines.launchedCollect
import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.inference_error_failed
import heron.ui.timeline.generated.resources.inference_error_model_not_loaded
import heron.ui.timeline.generated.resources.inference_sheet_translation_title
import heron.ui.timeline.generated.resources.inference_sheet_vibe_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

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
                        is InferenceAction.Generate -> action.flow.launchInferenceMutations(
                            state = state,
                            inferenceEngine = inferenceEngine,
                            inferenceModelManager = inferenceModelManager,
                            userDataRepository = userDataRepository,
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
private fun Flow<InferenceAction.Generate>.launchInferenceMutations(
    state: InferenceState.SnapshotMutable,
    inferenceEngine: InferenceEngine,
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
) = flatMapLatest { action ->
    when (action) {
        is InferenceAction.Generate.Translate -> inferenceEngine.outcomes(
            inferenceModelManager = inferenceModelManager,
            userDataRepository = userDataRepository,
            // Deterministic decoding for faithful, reproducible translations.
            params = GenerationParams(temperature = 0.6f),
            prompt = translationPrompt(
                text = action.post.record?.text.orEmpty(),
                targetLanguage = action.targetLanguage,
            ),
            transform = String::trim,
        ).map { InferenceKind.Translation to it }

        is InferenceAction.Generate.Vibe -> inferenceEngine.outcomes(
            inferenceModelManager = inferenceModelManager,
            userDataRepository = userDataRepository,
            prompt = vibePrompt(
                posts = action.posts,
                profile = action.profile,
            ),
            transform = String::trim,
        ).map { InferenceKind.Vibe to it }
    }
}.launchedCollect { (kind, outcome) ->
    state.kind = kind
    state.outcome = outcome
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
    if (state.value !is EngineState.Ready) {
        // Opportunistically load the selected default model; prompt the user when there is none.
        val loadedModel = resolveDefaultModel(
            inferenceModelManager = inferenceModelManager,
            userDataRepository = userDataRepository,
        )
        if (loadedModel == null) {
            emit(InferenceOutcome.NoModel)
            return@flow
        }
        // Loading an already loaded model is idempotent across engine implementations.
        load(loadedModel)
    }
    if (state.value !is EngineState.Ready) {
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

/**
 * Resolves the selected default model to a ready-to-load [LoadedModel], or null when no default is
 * set or that model has not been downloaded.
 */
private suspend fun resolveDefaultModel(
    inferenceModelManager: InferenceModelManager,
    userDataRepository: UserDataRepository,
): LoadedModel? {
    val defaultModelName = userDataRepository.preferences.first().local.defaultModelName
        ?: return null
    val model = inferenceModelManager.models.firstOrNull { it.name == defaultModelName }
        ?: return null
    return (inferenceModelManager.status(model).first() as? ModelStatus.Downloaded)
        ?.loadedModel
}

private fun translationPrompt(
    text: String,
    targetLanguage: String,
): String = """
    You are a translation engine. Translate the text between <src></src> into the IETF language tag
    $targetLanguage.
    Output the translation verbatim — no notes, no alternatives, no commentary, no quotes.
    <src>$text</src>
""".trimIndent()

private fun vibePrompt(
    posts: List<Post>,
    profile: Profile,
): String = buildString {
    appendLine(
        "Describe the \"vibe\" of a social media profile in one short, evocative phrase " +
            "(at most 12 words).",
    )
    appendLine("Base it on the author's bio and their recent posts, including any content labels.")
    appendLine("Reply with only the phrase — no preamble, no quotes.")
    appendLine()
    appendLine("Profile:")
    appendLine("- handle: @${profile.handle.id}")
    profile.displayName?.let { appendLine("- name: $it") }
    profile.description?.let { appendLine("- bio: $it") }
    profile.labels.takeIf { it.isNotEmpty() }?.let { labels ->
        appendLine("- labels: ${labels.joinToString { it.value.value }}")
    }
    appendLine()
    appendLine("Recent posts:")
    posts.forEach { post ->
        val postText = post.record?.text.orEmpty().replace("\n", " ").trim()
        val postLabels = post.labels.joinToString { it.value.value }
        if (postText.isNotEmpty() || postLabels.isNotEmpty()) {
            append("- \"$postText\"")
            if (postLabels.isNotEmpty()) append(" [labels: $postLabels]")
            appendLine()
        }
    }
}

@Stable
sealed interface InferenceOutcome {
    /** The text produced so far; empty until the first usable token arrives. */
    val text: String

    data class Loading(
        override val text: String = "",
    ) : InferenceOutcome

    data class Success(
        override val text: String,
    ) : InferenceOutcome

    data class Error(
        val memo: Memo,
        override val text: String = "",
    ) : InferenceOutcome

    /** No on-device model is available; the UI should prompt the user to download one. */
    data object NoModel : InferenceOutcome {
        override val text: String = ""
    }
}

/** What the engine is currently inferring; drives the inference sheet's title. */
enum class InferenceKind(
    val titleRes: StringResource,
) {
    Translation(titleRes = Res.string.inference_sheet_translation_title),
    Vibe(titleRes = Res.string.inference_sheet_vibe_title),
}

@Stable
@Snapshottable
interface InferenceState {
    @SnapshotSpec
    @Serializable
    data class Immutable(
        // Inference state is ephemeral runtime state mirroring the shared engine; never persisted.
        @Transient
        val engineState: EngineState? = null,
        @Transient
        val kind: InferenceKind? = null,
        @Transient
        val outcome: InferenceOutcome? = null,
    ) : InferenceState
}

sealed class InferenceAction(
    val key: String,
) {
    sealed class Generate : InferenceAction("Generate") {
        data class Translate(
            val post: Post,
            val targetLanguage: String,
        ) : Generate()

        data class Vibe(
            val posts: List<Post>,
            val profile: Profile,
        ) : Generate()
    }

    sealed class Navigate :
        InferenceAction(key = "Navigate"),
        NavigationAction {

        data class To(
            val delegate: NavigationAction.Destination,
        ) : Navigate(),
            NavigationAction by delegate
    }
}

private inline fun StringBuilder.transformedOrPlain(
    transform: (String) -> String,
): String {
    val string = toString()
    return transform(string).ifEmpty { string.trim() }
}
