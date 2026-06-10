package com.tunjid.heron.timeline.utilities

import com.tunjid.heron.timeline.ui.sheets.mutedwords.MutedWordsViewModelInitializer
import com.tunjid.heron.timeline.ui.sheets.postoptions.PostOptionsViewModelInitializer
import com.tunjid.heron.timeline.ui.sheets.threadgate.ThreadGateViewModelInitializer

class SheetsViewModelInitializers(
    val mutedWordsViewModelInitializer: MutedWordsViewModelInitializer,
    val postOptionsViewModelInitializer: PostOptionsViewModelInitializer,
    val threadGateViewModelInitializer: ThreadGateViewModelInitializer,
    // next sheet added here as migration continues
)
