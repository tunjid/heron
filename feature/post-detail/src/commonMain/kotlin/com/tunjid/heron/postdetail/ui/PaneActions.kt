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

package com.tunjid.heron.postdetail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import com.tunjid.heron.postdetail.Action
import com.tunjid.heron.postdetail.State
import com.tunjid.heron.postdetail.canTranslate
import com.tunjid.heron.postdetail.hasQuotePost
import com.tunjid.heron.sheets.rememberInferenceSheetState
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.scaffold.navigation.quoteThreadDestination
import com.tunjid.heron.ui.scaffold.scaffold.PaneScaffoldState
import heron.feature.post_detail.generated.resources.Res
import heron.feature.post_detail.generated.resources.translate_post_text
import heron.feature.post_detail.generated.resources.unroll_post_quotes
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaneScaffoldState.PaneActions(
    state: State,
    actions: (Action) -> Unit,
) {
    val inferenceSheetState = rememberInferenceSheetState()
    if (state.canTranslate) AppBarIconButton(
        icon = Icons.Rounded.Translate,
        iconDescription = stringResource(Res.string.translate_post_text),
        onClick = click@{
            val post = state.anchorPost ?: return@click
            val postLanguageTag = state.postLanguageTag ?: return@click
            val currentLanguageTag = state.currentLanguageTag ?: return@click
            inferenceSheetState.translate(
                post = post,
                sourceLanguage = postLanguageTag,
                targetLanguage = currentLanguageTag,
            )
        },
    )
    if (state.hasQuotePost) AppBarIconButton(
        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
        iconDescription = stringResource(Res.string.unroll_post_quotes),
        onClick = click@{
            state.anchorPost?.let { post ->
                actions(
                    Action.Navigate.To(
                        quoteThreadDestination(
                            post = post,
                            sharedElementPrefix = state.sharedElementPrefix,
                        ),
                    ),
                )
            }
        },
    )
    ThreadDisplayOptions(
        order = state.order,
        viewMode = state.viewMode,
        onOrderChanged = {
            actions(Action.Load.Order(it))
        },
        onViewModeChanged = {
            actions(Action.Load.ViewMode(it))
        },
    )
}
