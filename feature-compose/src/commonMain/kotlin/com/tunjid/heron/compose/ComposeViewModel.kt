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

package com.tunjid.heron.compose


import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.tunjid.heron.compose.di.creationType
import com.tunjid.heron.compose.di.sharedElementPrefix
import com.tunjid.heron.data.core.models.Post
import com.tunjid.heron.data.repository.AuthTokenRepository
import com.tunjid.heron.data.utilities.writequeue.Writable
import com.tunjid.heron.data.utilities.writequeue.WriteQueue
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.Mutation
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.mapToManyMutations
import com.tunjid.mutator.coroutines.mapToMutation
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

internal typealias ComposeStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class ComposeViewModelCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualComposeViewModel,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualComposeViewModel = creator.invoke(scope, route)
}

@Inject
class ActualComposeViewModel(
    navActions: (NavigationMutation) -> Unit,
    authTokenRepository: AuthTokenRepository,
    writeQueue: WriteQueue,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), ComposeStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        postText = TextFieldValue(
            AnnotatedString(
                when (val postType = route.creationType) {
                    is Post.Create.Mention -> "@${postType.profile.handle}"
                    is Post.Create.Reply -> ""
                    Post.Create.Timeline -> ""
                    null -> ""
                }
            )
        ),
        sharedElementPrefix = route.sharedElementPrefix,
        postType = route.creationType,
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
        loadSignedInProfileMutations(
            authTokenRepository = authTokenRepository,
        )
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {
                is Action.PostTextChanged -> action.flow.postTextMutations()
                is Action.SetFabExpanded -> action.flow.fabExpansionMutations()
                is Action.CreatePost -> action.flow.createPostMutations(
                    navActions = navActions,
                    writeQueue = writeQueue,
                )

                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)

private fun loadSignedInProfileMutations(
    authTokenRepository: AuthTokenRepository,
): Flow<Mutation<State>> =
    authTokenRepository.signedInUser.mapToMutation {
        copy(signedInProfile = it)
    }

private fun Flow<Action.PostTextChanged>.postTextMutations(
): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(postText = action.textFieldValue)
    }

private fun Flow<Action.SetFabExpanded>.fabExpansionMutations(
): Flow<Mutation<State>> =
    mapToMutation { action ->
        copy(fabExpanded = action.expanded)
    }

private fun Flow<Action.CreatePost>.createPostMutations(
    navActions: (NavigationMutation) -> Unit,
    writeQueue: WriteQueue,
): Flow<Mutation<State>> =
    mapToManyMutations { action ->
        val postWrite = Writable.Create(
            request = Post.Create.Request(
                authorId = action.authorId,
                text = action.text,
                links = action.links,
            ),
            replyTo = when (val postType = action.postType) {
                is Post.Create.Mention -> null
                is Post.Create.Reply -> postType
                Post.Create.Timeline -> null
                null -> null
            },
        )

        writeQueue.enqueue(postWrite)
        writeQueue.awaitDequeue(postWrite)
        emitAll(
            flowOf(Action.Navigate.Pop).consumeNavigationActions(
                navigationMutationConsumer = navActions
            )
        )
    }