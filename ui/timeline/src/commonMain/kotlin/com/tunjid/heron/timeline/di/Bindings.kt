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

package com.tunjid.heron.timeline.di

import com.tunjid.heron.data.di.DataBindings
import com.tunjid.heron.timeline.ui.sheets.embedrecordoptions.EmbeddableRecordOptionsViewModelInitializer
import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsViewModelInitializer
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsViewModelInitializer
import com.tunjid.heron.timeline.ui.sheets.threadgate.ThreadGateViewModelInitializer
import com.tunjid.heron.timeline.utilities.SheetsViewModelInitializers
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides

@BindingContainer
class SheetBindings(
    @Includes dataBindings: DataBindings,
) {
    @Provides
    fun provideSheetInitializers(
        mutedWordsViewModelInitializer: MutedWordsViewModelInitializer,
        postOptionsViewModelInitializer: PostOptionsViewModelInitializer,
        threadGateViewModelInitializer: ThreadGateViewModelInitializer,
        embeddableRecordOptionsViewModelInitializer: EmbeddableRecordOptionsViewModelInitializer,
    ): SheetsViewModelInitializers = SheetsViewModelInitializers(
        mutedWordsViewModelInitializer = mutedWordsViewModelInitializer,
        postOptionsViewModelInitializer = postOptionsViewModelInitializer,
        threadGateViewModelInitializer = threadGateViewModelInitializer,
        embeddableRecordOptionsViewModelInitializer = embeddableRecordOptionsViewModelInitializer,
    )
}
