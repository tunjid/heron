package com.tunjid.heron.timeline.ui.moderation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.rounded.Block
import androidx.compose.ui.graphics.vector.ImageVector
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.block_user
import heron.ui.timeline.generated.resources.mute_words
import org.jetbrains.compose.resources.StringResource

const val ModerationFeatureWhileSubscribed = 2_000L

sealed class ModerationOption(
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    data object MuteWords : ModerationOption(
        titleRes = Res.string.mute_words,
        icon = Icons.Default.FilterAlt,
    )

    data object BlockUser : ModerationOption(
        titleRes = Res.string.block_user,
        icon = Icons.Rounded.Block,
    )
}
