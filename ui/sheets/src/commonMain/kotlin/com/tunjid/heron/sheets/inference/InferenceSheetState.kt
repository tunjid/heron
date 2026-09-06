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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.data.ml.engine.EngineState
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.PlatformUnavailableReason
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.timeline.ui.icons.AtmosphereIcons
import com.tunjid.heron.ui.LoaderCurve
import com.tunjid.heron.ui.MorphingLoader
import com.tunjid.heron.ui.MorphingLoaderState.Companion.rememberMorphingLoaderState
import com.tunjid.heron.ui.Tab
import com.tunjid.heron.ui.Tabs
import com.tunjid.heron.ui.TabsState.Companion.rememberTabsState
import com.tunjid.heron.ui.scaffold.navigation.inferenceDestination
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.tabIndex
import com.tunjid.heron.ui.text.message
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.inference_disclaimer
import heron.ui.timeline.generated.resources.inference_no_model_description
import heron.ui.timeline.generated.resources.inference_no_model_title
import heron.ui.timeline.generated.resources.inference_phase_generating
import heron.ui.timeline.generated.resources.inference_phase_loading_model
import heron.ui.timeline.generated.resources.inference_phase_preparing
import heron.ui.timeline.generated.resources.inference_select_model_description
import heron.ui.timeline.generated.resources.inference_select_model_title
import heron.ui.timeline.generated.resources.inference_unavailable_ai_off_description
import heron.ui.timeline.generated.resources.inference_unavailable_ai_off_title
import heron.ui.timeline.generated.resources.inference_unavailable_preparing_description
import heron.ui.timeline.generated.resources.inference_unavailable_preparing_title
import heron.ui.timeline.generated.resources.posts
import heron.ui.timeline.generated.resources.replies
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
class InferenceSheetState internal constructor(
    scope: BottomSheetScope,
    internal val stateHolder: InferenceStateHolder,
) : BottomSheetState(scope) {

    fun translate(
        post: Post,
        sourceLanguage: String,
        targetLanguage: String,
    ) {
        stateHolder(
            InferenceAction.Translate(
                post = post,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
            ),
        )
        show()
    }

    fun profileVibeCheck(
        profileId: ProfileId,
    ) {
        stateHolder(
            InferenceAction.Vibe(
                profileId = profileId,
                type = Timeline.Profile.Type.Posts,
            ),
        )
        show()
    }

    fun spillTea(
        post: Post,
    ) {
        stateHolder(
            InferenceAction.Tea(
                post = post,
            ),
        )
        show()
    }

    override fun onHidden() = Unit

    companion object {
        @Composable
        fun rememberUpdatedInferenceSheetState(
            stateHolder: InferenceStateHolder,
        ): InferenceSheetState {
            val state = rememberBottomSheetState(
                skipPartiallyExpanded = false,
                stateHolder = stateHolder,
                block = ::InferenceSheetState,
            )
            state.ModalBottomSheet {
                InferenceBottomSheet(
                    state = state,
                )
            }
            return state
        }
    }
}

@Composable
internal fun InferenceBottomSheet(
    state: InferenceSheetState,
) {
    val inferenceState = state.stateHolder.produceState()
    val kind = inferenceState.kind
    val onNavigateToModels = {
        state.stateHolder(
            InferenceAction.Navigate.To(inferenceDestination()),
        )
    }
    val onSelectModel: (LoadedModel) -> Unit = { model ->
        state.stateHolder(
            InferenceAction.SelectDefaultModel(model),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val headerOutcome = when (kind) {
            InferenceKind.Vibe -> inferenceState.postsOutcome
            InferenceKind.Translation -> inferenceState.translationOutcome
            InferenceKind.Tea -> inferenceState.teaOutcome
        }
        if (!headerOutcome.isFullBleedPrompt) {
            Text(
                text = stringResource(kind.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.padding(
                    vertical = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = AtmosphereIcons.Help,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(Res.string.inference_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        AnimatedContent(
            targetState = kind,
        ) { targetKind ->
            when (targetKind) {
                InferenceKind.Translation -> InferenceOutcomeContent(
                    outcome = inferenceState.translationOutcome,
                    engineState = inferenceState.engineState,
                    onNavigateToModels = onNavigateToModels,
                    onSelectModel = onSelectModel,
                )
                InferenceKind.Tea -> InferenceOutcomeContent(
                    outcome = inferenceState.teaOutcome,
                    engineState = inferenceState.engineState,
                    onNavigateToModels = onNavigateToModels,
                    onSelectModel = onSelectModel,
                )
                InferenceKind.Vibe -> ProfileVibeContent(
                    profileId = inferenceState.vibeProfileId,
                    postsOutcome = inferenceState.postsOutcome,
                    repliesOutcome = inferenceState.repliesOutcome,
                    engineState = inferenceState.engineState,
                    onSelectLens = { profileId, type ->
                        state.stateHolder(
                            InferenceAction.Vibe(
                                profileId = profileId,
                                type = type,
                            ),
                        )
                    },
                    onNavigateToModels = onNavigateToModels,
                    onSelectModel = onSelectModel,
                )
            }
        }
        Spacer(
            modifier = Modifier
                .height(24.dp)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun ProfileVibeContent(
    profileId: ProfileId?,
    postsOutcome: InferenceOutcome?,
    repliesOutcome: InferenceOutcome?,
    engineState: EngineState?,
    onSelectLens: (ProfileId, Timeline.Profile.Type) -> Unit,
    onNavigateToModels: () -> Unit,
    onSelectModel: (LoadedModel) -> Unit,
) {
    // With no usable model, a single prompt reads better than one repeated behind every tab.
    if (postsOutcome.isFullBleedPrompt) {
        InferenceOutcomeContent(
            outcome = postsOutcome,
            engineState = engineState,
            onNavigateToModels = onNavigateToModels,
            onSelectModel = onSelectModel,
        )
        return
    }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { VibeTab.entries.size }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Tabs(
            tabsState = rememberTabsState(
                tabs = VibeTab.entries.map { vibeTab ->
                    Tab(
                        title = stringResource(vibeTab.titleRes),
                        hasUpdate = false,
                    )
                },
                selectedTabIndex = pagerState::tabIndex,
                onTabSelected = { page ->
                    scope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                onTabReselected = {},
            ),
        )
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth(),
            state = pagerState,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                InferenceOutcomeContent(
                    outcome = when (VibeTab.entries[page]) {
                        VibeTab.Posts -> postsOutcome
                        VibeTab.Replies -> repliesOutcome
                    },
                    engineState = engineState,
                    onNavigateToModels = onNavigateToModels,
                    onSelectModel = onSelectModel,
                )
            }
        }

        val currentProfileId by rememberUpdatedState(profileId)
        LaunchedEffect(
            pagerState,
            profileId,
        ) {
            snapshotFlow { pagerState.settledPage }
                .collect { page ->
                    val type = VibeTab.entries
                        .getOrNull(page)
                        ?.type
                        ?: return@collect
                    currentProfileId?.let { onSelectLens(it, type) }
                }
        }
    }
}

@Composable
private fun InferenceOutcomeContent(
    outcome: InferenceOutcome?,
    engineState: EngineState?,
    onNavigateToModels: () -> Unit,
    onSelectModel: (LoadedModel) -> Unit,
) {
    when (outcome) {
        null,
        is InferenceOutcome.Loading,
        is InferenceOutcome.Success,
        -> outcome?.text.orEmpty().let { text ->
            // Until the first token lands there is nothing to show, so the engine's phase becomes the
            // exposition beside the spinner: preparing input, warming the model, or generating.
            val loading = text.isBlank() && outcome is InferenceOutcome.Loading

            AnimatedContent(
                targetState = loading,
                label = "InferenceLoadingTransition",
                transitionSpec = {
                    FadeInAndOut
                },
            ) { currentlyLoading ->
                if (currentlyLoading)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MorphingLoader(
                            state = rememberMorphingLoaderState(
                                curve = LoaderCurve.Rose,
                                energy = engineState.loaderEnergy,
                                cometLength = 0.012f,
                                cometDurationMillis = 8000,
                            ),
                            modifier = Modifier
                                .size(28.dp),
                        )
                        Text(
                            text = stringResource(engineState.loadingCaptionRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                else Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        is InferenceOutcome.NoModel -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToModels),
        ) {
            EmptyContent(
                titleRes = Res.string.inference_no_model_title,
                descriptionRes = Res.string.inference_no_model_description,
                icon = Icons.Rounded.Download,
            )
        }

        is InferenceOutcome.SelectDefault -> SelectDefaultModelContent(
            models = outcome.models,
            onSelectModel = onSelectModel,
        )

        is InferenceOutcome.Unavailable -> EmptyContent(
            modifier = Modifier.fillMaxWidth(),
            titleRes = when (outcome.reason) {
                PlatformUnavailableReason.AppleIntelligenceDisabled ->
                    Res.string.inference_unavailable_ai_off_title
                PlatformUnavailableReason.ModelDownloading ->
                    Res.string.inference_unavailable_preparing_title
            },
            descriptionRes = when (outcome.reason) {
                PlatformUnavailableReason.AppleIntelligenceDisabled ->
                    Res.string.inference_unavailable_ai_off_description
                PlatformUnavailableReason.ModelDownloading ->
                    Res.string.inference_unavailable_preparing_description
            },
            icon = Icons.Rounded.AutoAwesome,
        )

        is InferenceOutcome.Error -> Text(
            text = outcome.memo.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SelectDefaultModelContent(
    models: List<LoadedModel>,
    onSelectModel: (LoadedModel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = 24.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(36.dp),
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.inference_select_model_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.inference_select_model_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        // One button per downloaded model; tapping adopts it as this account's default and resumes
        // the pending inference on it.
        models.forEach { model ->
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelectModel(model) },
            ) {
                Text(text = model.model.name)
            }
        }
    }
}

private val InferenceOutcome?.isFullBleedPrompt: Boolean
    get() = this is InferenceOutcome.NoModel ||
        this is InferenceOutcome.SelectDefault ||
        this is InferenceOutcome.Unavailable

private fun EngineState?.loadingCaptionRes(): StringResource = when (this) {
    is EngineState.Loading -> Res.string.inference_phase_loading_model
    is EngineState.Ready.Streaming -> Res.string.inference_phase_generating
    else -> Res.string.inference_phase_preparing
}

private val EngineState?.loaderEnergy: Float
    get() = when (this) {
        is EngineState.Ready.Streaming -> 0.3f
        is EngineState.Loading -> 0.1f
        else -> 0f
    }

private enum class VibeTab(
    val type: Timeline.Profile.Type,
    val titleRes: StringResource,
) {
    Posts(
        type = Timeline.Profile.Type.Posts,
        titleRes = Res.string.posts,
    ),
    Replies(
        type = Timeline.Profile.Type.Replies,
        titleRes = Res.string.replies,
    ),
}

private val FadeInAndOut = fadeIn() togetherWith fadeOut()
