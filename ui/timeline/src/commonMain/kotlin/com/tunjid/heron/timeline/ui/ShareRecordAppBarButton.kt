package com.tunjid.heron.timeline.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleUp
import androidx.compose.runtime.Composable
import com.tunjid.heron.ui.AppBarIconButton
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShareRecordAppBarButton(
    contentDescription: String,
    onShareClicked: () -> Unit,
) {
    AppBarIconButton(
        icon = Icons.Rounded.ArrowCircleUp,
        iconDescription = contentDescription,
        onClick = onShareClicked,
    )
}
