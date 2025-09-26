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

package com.tunjid.heron.signin

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.signin.oauth.rememberOauthFlowState
import com.tunjid.heron.signin.ui.FormField
import com.tunjid.heron.signin.ui.ServerSelection
import com.tunjid.heron.signin.ui.ServerSelectionSheetState.Companion.rememberUpdatedServerSelectionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SignInScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 360.dp)
            .padding(horizontal = 56.dp)
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val oauthFlowUri = rememberUpdatedState(state.oauthRequestUri)
        val currentProfileHandle = rememberUpdatedState(state.profileHandle)

        val oauthFlowState = rememberOauthFlowState { result ->
            actions(
                Action.OauthFlowResultAvailable(
                    handle = currentProfileHandle.value,
                    result = result,
                ),
            )
        }

        state.fields.forEach { field ->
            key(field.id) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isVisible(field),
                    enter = EnterTransition,
                    exit = ExitTransition,
                ) {
                    FormField(
                        modifier = Modifier
                            .fillMaxWidth(),
                        field = field,
                        onValueChange = { field, newValue ->
                            actions(
                                Action.FieldChanged(
                                    id = field.id,
                                    text = newValue,
                                ),
                            )
                        },
                        keyboardActions = {
                            when (it.id) {
                                Username -> focusManager.moveFocus(
                                    focusDirection = FocusDirection.Next,
                                )

                                Password -> if (state.submitButtonEnabled) {
                                    actions(state.createSessionAction())
                                    keyboardController?.hide()
                                }
                            }
                        },
                    )
                }
            }
        }

        val serverSelectionSheetState = rememberUpdatedServerSelectionState(
            onServerConfirmed = {
                actions(Action.SetServer(it))
            },
        )

        ServerSelection(
            modifier = Modifier
                .align(Alignment.End)
                .animateBounds(paneScaffoldState),
            selectedServer = state.selectedServer,
            availableServers = state.availableServers,
            onServerSelected = serverSelectionSheetState::onServer,
        )

        LaunchedEffect(Unit) {
            launch {
                snapshotFlow { oauthFlowState.supportsOauth }
                    .collectLatest { supportsOauth ->
                        actions(Action.OauthAvailabilityChanged(supportsOauth))
                    }
            }
            launch {
                snapshotFlow { oauthFlowUri.value }
                    .filterNotNull()
                    .collectLatest(oauthFlowState::launch)
            }
        }
    }
}

private val ExitTransition = slideOutVertically { -it } + fadeOut()
private val EnterTransition = fadeIn() + slideInVertically { -it }
