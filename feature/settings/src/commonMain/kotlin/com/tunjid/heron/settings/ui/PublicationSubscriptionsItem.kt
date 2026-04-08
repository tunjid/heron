package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.publication_subscriptions
import org.jetbrains.compose.resources.stringResource

@Composable
fun PublicationSubscriptionsItem(
    modifier: Modifier = Modifier,
    onPublicationSubscriptionsClicked: () -> Unit,
) {
    SettingsItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onPublicationSubscriptionsClicked()
            }
            .settingsItemPaddingAndMinHeight(),
        title = stringResource(Res.string.publication_subscriptions),
        icon = Icons.Rounded.Newspaper,
    )
}
