package com.tunjid.heron.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import heron.feature.settings.generated.resources.Res
import heron.feature.settings.generated.resources.inference
import org.jetbrains.compose.resources.stringResource

@Composable
fun InferenceItem(
    modifier: Modifier = Modifier,
    onInferenceClicked: () -> Unit,
) {
    SettingsItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onInferenceClicked()
            }
            .settingsItemPaddingAndMinHeight(),
        title = stringResource(Res.string.inference),
        icon = Icons.Rounded.AutoAwesome,
    )
}
