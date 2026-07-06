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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.core.models.Profile
import com.tunjid.heron.timeline.ui.EmptyContent
import com.tunjid.heron.ui.scaffold.navigation.inferenceDestination
import com.tunjid.heron.ui.sheets.BottomSheetScope
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.ModalBottomSheet
import com.tunjid.heron.ui.sheets.BottomSheetScope.Companion.rememberBottomSheetState
import com.tunjid.heron.ui.sheets.BottomSheetState
import com.tunjid.heron.ui.text.message
import com.tunjid.mutator.compose.produceState
import com.tunjid.mutator.invoke
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.inference_no_model_description
import heron.ui.timeline.generated.resources.inference_no_model_title
import org.jetbrains.compose.resources.stringResource

@Stable
class InferenceSheetState internal constructor(
    scope: BottomSheetScope,
    internal val stateHolder: InferenceStateHolder,
) : BottomSheetState(scope) {

    fun translate(
        post: Post,
        targetLanguage: String,
    ) {
        stateHolder(
            InferenceAction.Generate.Translate(
                post = post,
                targetLanguage = targetLanguage,
            ),
        )
        show()
    }

    fun vibe(
        posts: List<Post>,
        profile: Profile,
    ) {
        stateHolder(
            InferenceAction.Generate.Vibe(
                posts = posts,
                profile = profile,
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
        val outcome = inferenceState.outcome
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The no-model prompt carries its own title, so only show the kind header otherwise.
            if (outcome !is InferenceOutcome.NoModel) {
                inferenceState.kind?.let { kind ->
                    Text(
                        text = stringResource(kind.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            InferenceOutcomeContent(
                outcome = outcome,
                onNavigateToModels = {
                    state.stateHolder(
                        InferenceAction.Navigate.To(inferenceDestination()),
                    )
                },
            )
            Spacer(
                modifier = Modifier
                    .height(24.dp)
                    .navigationBarsPadding(),
            )
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
        -> Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            val partial = outcome?.text.orEmpty()
            if (partial.isNotEmpty()) {
                Text(
                    text = partial,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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

        is InferenceOutcome.Success -> Text(
            text = outcome.text,
            style = MaterialTheme.typography.bodyMedium,
        )

        is InferenceOutcome.Error -> Text(
            text = outcome.memo.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
