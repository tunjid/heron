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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Timeline
import com.tunjid.heron.data.core.types.ProfileId
import com.tunjid.heron.timeline.ui.EmptyContent
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
import heron.ui.timeline.generated.resources.inference_no_model_description
import heron.ui.timeline.generated.resources.inference_no_model_title
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
            InferenceBottomSheet(
                state = state,
            )
            return state
        }
    }
}

@Composable
private fun InferenceBottomSheet(
    state: InferenceSheetState,
) {
    state.ModalBottomSheet {
        val inferenceState = state.stateHolder.produceState()
        val kind = inferenceState.kind
        val onNavigateToModels = {
            state.stateHolder(
                InferenceAction.Navigate.To(inferenceDestination()),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The outcome for the kind on screen decides whether the header is shown; vibe reads its
            // posts slot since both vibe slots share the same no-model state.
            val headerOutcome = when (kind) {
                InferenceKind.Vibe -> inferenceState.postsOutcome
                InferenceKind.Translation -> inferenceState.translationOutcome
            }
            // The no-model prompt carries its own title, so only show the kind header otherwise.
            if (headerOutcome !is InferenceOutcome.NoModel) {
                Text(
                    text = stringResource(kind.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AnimatedContent(
                targetState = kind,
            ) { targetKind ->
                when (targetKind) {
                    InferenceKind.Translation -> InferenceOutcomeContent(
                        outcome = inferenceState.translationOutcome,
                        onNavigateToModels = onNavigateToModels,
                    )
                    InferenceKind.Vibe -> ProfileVibeContent(
                        profileId = inferenceState.vibeProfileId,
                        postsOutcome = inferenceState.postsOutcome,
                        repliesOutcome = inferenceState.repliesOutcome,
                        onSelectLens = { profileId, type ->
                            state.stateHolder(
                                InferenceAction.Vibe(
                                    profileId = profileId,
                                    type = type,
                                ),
                            )
                        },
                        onNavigateToModels = onNavigateToModels,
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
}

@Composable
private fun ProfileVibeContent(
    profileId: ProfileId?,
    postsOutcome: InferenceOutcome?,
    repliesOutcome: InferenceOutcome?,
    onSelectLens: (ProfileId, Timeline.Profile.Type) -> Unit,
    onNavigateToModels: () -> Unit,
) {
    // With no model loaded, a single prompt reads better than one repeated behind every tab.
    if (postsOutcome is InferenceOutcome.NoModel) {
        InferenceOutcomeContent(
            outcome = postsOutcome,
            onNavigateToModels = onNavigateToModels,
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
                    onNavigateToModels = onNavigateToModels,
                )
            }
        }

        val currentProfileId by rememberUpdatedState(profileId)
        LaunchedEffect(pagerState) {
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
    onNavigateToModels: () -> Unit,
) {
    when (outcome) {
        null,
        is InferenceOutcome.Loading,
        is InferenceOutcome.Success,
        -> outcome?.text.orEmpty().let { text ->
            if (text.isBlank() && outcome is InferenceOutcome.Loading) CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            else Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        is InferenceOutcome.NoModel -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clickable(onClick = onNavigateToModels),
        ) {
            EmptyContent(
                titleRes = Res.string.inference_no_model_title,
                descriptionRes = Res.string.inference_no_model_description,
                icon = Icons.Rounded.Download,
            )
        }

        is InferenceOutcome.Error -> Text(
            text = outcome.memo.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
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
