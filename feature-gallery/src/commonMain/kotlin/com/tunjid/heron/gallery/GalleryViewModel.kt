/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.heron.gallery


import androidx.lifecycle.ViewModel
import com.tunjid.heron.data.core.models.ImageList
import com.tunjid.heron.data.core.models.Video
import com.tunjid.heron.feature.AssistedViewModelFactory
import com.tunjid.heron.feature.FeatureWhileSubscribed
import com.tunjid.heron.gallery.di.media
import com.tunjid.heron.gallery.di.sharedElementPrefix
import com.tunjid.heron.gallery.di.startIndex
import com.tunjid.heron.scaffold.navigation.NavigationMutation
import com.tunjid.heron.scaffold.navigation.consumeNavigationActions
import com.tunjid.mutator.ActionStateMutator
import com.tunjid.mutator.coroutines.actionStateFlowMutator
import com.tunjid.mutator.coroutines.toMutationStream
import com.tunjid.treenav.strings.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

typealias GalleryStateHolder = ActionStateMutator<Action, StateFlow<State>>

@Inject
class GalleryStateHolderCreator(
    private val creator: (scope: CoroutineScope, route: Route) -> ActualGalleryStateHolder,
) : AssistedViewModelFactory {
    override fun invoke(
        scope: CoroutineScope,
        route: Route,
    ): ActualGalleryStateHolder = creator.invoke(scope, route)
}

@Inject
class ActualGalleryStateHolder(
    navActions: (NavigationMutation) -> Unit,
    @Assisted
    scope: CoroutineScope,
    @Assisted
    route: Route,
) : ViewModel(viewModelScope = scope), GalleryStateHolder by scope.actionStateFlowMutator(
    initialState = State(
        startIndex = route.startIndex,
        sharedElementPrefix = route.sharedElementPrefix,
        items = when (val media = route.media) {
            is ImageList -> media.images.map(GalleryItem::Photo)
            is Video -> emptyList()
            null -> emptyList()
        }
    ),
    started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed),
    inputs = listOf(
    ),
    actionTransform = transform@{ actions ->
        actions.toMutationStream(
            keySelector = Action::key
        ) {
            when (val action = type()) {


                is Action.Navigate -> action.flow.consumeNavigationActions(
                    navigationMutationConsumer = navActions
                )
            }
        }
    }
)
