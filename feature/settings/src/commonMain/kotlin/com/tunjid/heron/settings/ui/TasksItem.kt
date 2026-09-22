package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.CloudSync
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.tasks
import org.jetbrains.compose.resources.stringResource

@Composable
fun TasksItem(
    modifier: Modifier = Modifier,
    onTasksClicked: () -> Unit,
) {
    SettingsItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onTasksClicked()
            }
            .settingsItemPaddingAndMinHeight(),
        title = stringResource(Res.string.tasks),
        icon = HeronIcons.Regular.CloudSync,
    )
}
