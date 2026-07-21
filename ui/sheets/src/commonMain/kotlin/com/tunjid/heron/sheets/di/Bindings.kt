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

package com.tunjid.heron.sheets.di

import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsStateHolder
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsViewModelInitializer
import com.tunjid.heron.sheets.inference.InferenceStateHolder
import com.tunjid.heron.sheets.inference.InferenceViewModelInitializer
import com.tunjid.heron.sheets.mutedwords.MutedWordsStateHolder
import com.tunjid.heron.sheets.mutedwords.MutedWordsViewModelInitializer
import com.tunjid.heron.sheets.postinteractions.PostInteractionsStateHolder
import com.tunjid.heron.sheets.postinteractions.PostInteractionsViewModelInitializer
import com.tunjid.heron.sheets.postoptions.PostOptionsStateHolder
import com.tunjid.heron.sheets.postoptions.PostOptionsViewModelInitializer
import com.tunjid.heron.sheets.profile.ProfileSearchStateHolder
import com.tunjid.heron.sheets.profile.ProfileSearchViewModelInitializer
import com.tunjid.heron.sheets.selectlist.SelectListStateHolder
import com.tunjid.heron.sheets.selectlist.SelectListViewModelInitializer
import com.tunjid.heron.sheets.threadgate.ThreadGateStateHolder
import com.tunjid.heron.sheets.threadgate.ThreadGateViewModelInitializer
import com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Contributes each sheet's assisted [com.tunjid.heron.ui.stateproduction.SheetStateHolderInitializer]
 * into the app graph's `Map<KClass<*>, SheetStateHolderInitializer>`, keyed by the sheet's state
 * holder interface. This mirrors how feature modules contribute their navigation entries, and lets
 * `PaneScaffoldState` resolve a sheet's state holder without the scaffold layer depending on this
 * module.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object SheetBindings {
    @Provides
    @IntoMap
    @ClassKey(MutedWordsStateHolder::class)
    fun provideMutedWordsViewModelInitializer(
        initializer: MutedWordsViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(PostOptionsStateHolder::class)
    fun providePostOptionsViewModelInitializer(
        initializer: PostOptionsViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(ThreadGateStateHolder::class)
    fun provideThreadGateViewModelInitializer(
        initializer: ThreadGateViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(EmbeddableRecordOptionsStateHolder::class)
    fun provideEmbeddableRecordOptionsViewModelInitializer(
        initializer: EmbeddableRecordOptionsViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(SelectListStateHolder::class)
    fun provideSelectListViewModelInitializer(
        initializer: SelectListViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(PostInteractionsStateHolder::class)
    fun providePostInteractionsViewModelInitializer(
        initializer: PostInteractionsViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(InferenceStateHolder::class)
    fun provideInferenceViewModelInitializer(
        initializer: InferenceViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(ProfileSearchStateHolder::class)
    internal fun provideProfileSearchSViewModelInitializer(
        initializer: ProfileSearchViewModelInitializer,
    ): SheetStateHolderInitializer =
        SheetStateHolderInitializer(initializer::invoke)
}
