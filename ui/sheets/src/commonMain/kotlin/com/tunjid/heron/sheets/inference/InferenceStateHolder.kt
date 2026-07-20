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
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.models.TimelineItem
import com.tunjid.heron.data.core.models.flattenedText
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.engine.GenerationParams
import com.tunjid.heron.data.ml.engine.InferenceEngine
import com.tunjid.heron.data.ml.language.englishDisplayName
import com.tunjid.heron.data.ml.model.InferenceModelManager
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.ModelStatus
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
            cursor = Cursor.Initial,
        )
            .mapNotNull { cursorList ->
                cursorList.items.takeIf(List<TimelineItem>::isNotEmpty)
            }
            .first()
    }
        .orEmpty()

private fun translationPrompt(
    text: String,
    sourceLanguageTag: String,
    targetLanguageTag: String,
): String {
    val sourceLanguage = englishDisplayName(sourceLanguageTag)
    val targetLanguage = englishDisplayName(targetLanguageTag)
    return """
        Translate the text between <text> tags from $sourceLanguage to $targetLanguage.
        Reply with only the $targetLanguage translation: no quotes, no notes, no explanation.

        <text>
        $text
        </text>

        translation in $targetLanguage:
    """.trimIndent()
}

private fun vibePrompt(
    items: List<TimelineItem>,
    profile: Profile,
    type: Timeline.Profile.Type,
): String = buildString {
    // The two lenses read a profile differently: their posts show the voice they broadcast in,
    // their replies show how they behave in conversation. Frame the ask and the samples to match.
    val basis: String
    val samplesHeader: String
    when (type) {
        Timeline.Profile.Type.Replies -> {
            basis = "how they show up in replies to other people — their conversational tone, " +
                "their wit, and how they treat the people they talk to"
            samplesHeader = "Recent replies, shown within the conversations they belong to " +
                "(the author's own lines are marked):"
        }
        else -> {
            basis = "their recent posts — the topics they gravitate to and the voice they post in"
            samplesHeader = "Recent posts:"
        }
    }
    appendLine("Describe the \"vibe\" of a social media profile in two or three short sentences.")
    appendLine("Base it on the author's bio and $basis, including any content labels.")
    appendLine("Reply with only the description — no preamble, no headings, no quotes.")
    appendLine()
    appendLine("Profile:")
    appendLine("- handle: @${profile.handle.id}")
    profile.displayName?.let { appendLine("- name: $it") }
    profile.description?.let { appendLine("- bio: $it") }
    profile.labels.takeIf { it.isNotEmpty() }?.let { labels ->
        appendLine("- labels: ${labels.joinToString { it.value.value }}")
    }
    appendLine()
    appendLine(samplesHeader)
    items.forEach { item ->
        appendVibeSample(
            item = item,
            authorId = profile.did,
        )
    }
}

/**
 * Appends one recent [item] as a vibe sample. Standalone posts render as a single line; reply and
 * quote threads render as an indented conversation so the model sees the context [authorId] is
 * responding to, with the author's own lines marked.
 */
private fun StringBuilder.appendVibeSample(
    item: TimelineItem,
    authorId: ProfileId,
) {
    when (item) {
        is TimelineItem.Single,
        is TimelineItem.Pinned,
        -> appendVibePost(
            prefix = "- ",
            post = item.post,
            authorId = authorId,
            withHandle = false,
        )

        is TimelineItem.Repost -> appendVibePost(
            prefix = "- reposted ",
            post = item.post,
            authorId = authorId,
            withHandle = true,
        )

        is TimelineItem.Threaded.Linear -> appendVibeConversation(
            posts = item.nodes.map { it.post },
            authorId = authorId,
        )

        is TimelineItem.Threaded.Tree -> appendVibeConversation(
            posts = listOf(item.anchor.post) + item.replies.map { it.post },
            authorId = authorId,
        )

        is TimelineItem.Placeholder -> Unit
    }
}

private fun StringBuilder.appendVibeConversation(
    posts: List<Post>,
    authorId: ProfileId,
) {
    if (posts.none { it.hasVibeContent }) return
    appendLine("- conversation:")
    posts.forEach { post ->
        appendVibePost(
            prefix = "    ",
            post = post,
            authorId = authorId,
            withHandle = true,
        )
    }
}

private fun StringBuilder.appendVibePost(
    prefix: String,
    post: Post,
    authorId: ProfileId,
    withHandle: Boolean,
) {
    if (!post.hasVibeContent) return
    append(prefix)
    if (withHandle) {
        append("@${post.author.handle.id}")
        if (post.author.did == authorId) append(" (the author)")
        append(": ")
    }
    append("\"${post.flattenedText.orEmpty()}\"")
    val labels = post.labels.joinToString { it.value.value }
    if (labels.isNotEmpty()) append(" [labels: $labels]")
    appendLine()
}

private val Post.hasVibeContent: Boolean
    get() = !flattenedText.isNullOrEmpty() || labels.isNotEmpty()

private inline fun StringBuilder.transformedOrPlain(
    transform: (String) -> String,
): String {
    val string = toString()
    return transform(string).ifEmpty { string.trim() }
}

/**
 * Strips wrappers a model may add around a translation despite instructions — surrounding code
 * fences or quotes — so the sheet always renders plain text. Only removes a wrapper present on
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
