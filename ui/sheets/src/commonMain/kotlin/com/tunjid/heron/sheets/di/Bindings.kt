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

import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsViewModel
import com.tunjid.heron.sheets.embedrecordoptions.EmbeddableRecordOptionsViewModelInitializer
import com.tunjid.heron.sheets.mutedwords.MutedWordsViewModel
import com.tunjid.heron.sheets.mutedwords.MutedWordsViewModelInitializer
import com.tunjid.heron.sheets.postinteractions.PostInteractionsViewModel
import com.tunjid.heron.sheets.postinteractions.PostInteractionsViewModelInitializer
import com.tunjid.heron.sheets.postoptions.PostOptionsViewModel
import com.tunjid.heron.sheets.postoptions.PostOptionsViewModelInitializer
import com.tunjid.heron.sheets.selectlist.SelectListViewModel
import com.tunjid.heron.sheets.selectlist.SelectListViewModelInitializer
import com.tunjid.heron.sheets.threadgate.ThreadGateViewModel
import com.tunjid.heron.sheets.threadgate.ThreadGateViewModelInitializer
import com.tunjid.heron.ui.coroutines.SheetViewModelInitializer
import com.tunjid.heron.ui.scaffold.di.ScaffoldBindings
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Contributes each sheet [com.tunjid.heron.ui.coroutines.SheetViewModel]'s assisted initializer into
 * the app graph's `Map<KClass<out SheetViewModel>, SheetViewModelInitializer>`, keyed by the
 * ViewModel class. This mirrors how feature modules contribute their navigation entries, and lets
 * `PaneScaffoldState` create a sheet ViewModel reflectively without the scaffold layer depending on
 * this module.
 */
@BindingContainer
class SheetBindings(
    @Includes val dataBindings: DataBindings,
    @Includes val scaffoldBindings: ScaffoldBindings,
) {
    @Provides
    @IntoMap
    @ClassKey(MutedWordsViewModel::class)
    fun provideMutedWordsViewModelInitializer(
        initializer: MutedWordsViewModelInitializer,
    ): SheetViewModelInitializer =
        SheetViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(PostOptionsViewModel::class)
    fun providePostOptionsViewModelInitializer(
        initializer: PostOptionsViewModelInitializer,
    ): SheetViewModelInitializer =
        SheetViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(ThreadGateViewModel::class)
    fun provideThreadGateViewModelInitializer(
        initializer: ThreadGateViewModelInitializer,
    ): SheetViewModelInitializer =
        SheetViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(EmbeddableRecordOptionsViewModel::class)
    fun provideEmbeddableRecordOptionsViewModelInitializer(
        initializer: EmbeddableRecordOptionsViewModelInitializer,
    ): SheetViewModelInitializer =
        SheetViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(SelectListViewModel::class)
    fun provideSelectListViewModelInitializer(
        initializer: SelectListViewModelInitializer,
    ): SheetViewModelInitializer =
        SheetViewModelInitializer(initializer::invoke)

    @Provides
    @IntoMap
    @ClassKey(PostInteractionsViewModel::class)
    fun providePostInteractionsViewModelInitializer(
        initializer: PostInteractionsViewModelInitializer,
    ): SheetViewModelInitializer =
        SheetViewModelInitializer(initializer::invoke)
}
