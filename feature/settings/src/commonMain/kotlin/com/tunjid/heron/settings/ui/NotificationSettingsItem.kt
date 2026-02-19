package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.notification_settings
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationSettingsItem(
    modifier: Modifier = Modifier,
    onNotificationSettingsClicked: () -> Unit,
) {
    SettingsItemRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onNotificationSettingsClicked()
            }
            .settingsItemPaddingAndMinHeight(),
        title = stringResource(CommonStrings.notification_settings),
        icon = Icons.Rounded.Notifications,
    )
}
