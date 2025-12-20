package com.tunjid.heron.timeline.ui.moderation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.rounded.Block
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tunjid.heron.timeline.ui.sheets.MutedWordsStateHolder

sealed class ModerationOption(
    val title: String,
    val icon: ImageVector,
) {
    data object MuteWords : ModerationOption(
        title = "Mute words and tags",
        icon = Icons.Default.FilterAlt,
    )

    data object BlockUser : ModerationOption(
        title = "Block user",
        icon = Icons.Rounded.Block,
    )
}

@Stable
data class ModerationState(
    val mutedWordsStateHolder: MutedWordsStateHolder? = null,
    // Add more moderation state holders as needed
)
