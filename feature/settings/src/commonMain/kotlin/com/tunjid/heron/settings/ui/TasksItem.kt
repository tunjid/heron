package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        icon = Icons.Rounded.CloudSync,
    )
}
