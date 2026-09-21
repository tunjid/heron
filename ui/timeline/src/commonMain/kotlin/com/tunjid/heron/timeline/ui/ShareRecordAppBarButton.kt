package com.tunjid.heron.timeline.ui

import androidx.compose.runtime.Composable
import com.tunjid.heron.ui.AppBarIconButton
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.ArrowCircleUp
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShareRecordAppBarButton(
    contentDescription: String,
    onShareClicked: () -> Unit,
) {
    AppBarIconButton(
        icon = HeronIcons.Regular.ArrowCircleUp,
        iconDescription = contentDescription,
        onClick = onShareClicked,
    )
}
