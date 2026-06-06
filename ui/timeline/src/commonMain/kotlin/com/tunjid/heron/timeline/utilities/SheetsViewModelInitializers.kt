package com.tunjid.heron.timeline.utilities

import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsViewModelInitializer
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsViewModelInitializer

class SheetsViewModelInitializers(
    val mutedWordsViewModelInitializer: MutedWordsViewModelInitializer,
    val postOptionsViewModelInitializer: PostOptionsViewModelInitializer,
    // next sheet added here as migration continues
)
