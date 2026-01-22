/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.notificationsettings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.NotificationPreferences
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.notification_settings.generated.resources.Res
import heron.feature.notification_settings.generated.resources.everyone
import heron.feature.notification_settings.generated.resources.in_app
import heron.feature.notification_settings.generated.resources.off
import heron.feature.notification_settings.generated.resources.people_you_follow
import heron.feature.notification_settings.generated.resources.push
import heron.feature.notification_settings.generated.resources.push_and_in_app
import heron.ui.core.generated.resources.collapse_icon
import heron.ui.core.generated.resources.expand_icon
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationSettingsRadioButton(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = { if (!selected) onSelect() },
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun SettingsItemRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    secondary: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = SettingsItemClipModifier
            .then(
                modifier
                    .semantics { contentDescription = title }
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = titleColor,
                style = MaterialTheme.typography.bodyLarge,
            )

            if (secondary != null) {
                Spacer(Modifier.height(2.dp))
                secondary()
            }
        }

        trailing()
    }
}

@Composable
internal fun ExpandableSettingsItemRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit = {},
    status: (@Composable () -> Unit)? = null,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = SettingsItemClipModifier
            .then(
                modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isExpanded,
                        onValueChange = { isExpanded = it },
                        role = Role.Button,
                    ),
            ),
    ) {
        SettingsItemRow(
            modifier = Modifier
                .fillMaxWidth(),
            title = title,
            icon = icon,
            titleColor = titleColor,
            secondary = if (!isExpanded) status else null,
        ) {
            val iconRotation = animateFloatAsState(
                targetValue = if (isExpanded) 0f
                else 180f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            Icon(
                modifier = Modifier.graphicsLayer {
                    rotationX = iconRotation.value
                },
                imageVector = Icons.Default.ExpandLess,
                contentDescription = stringResource(
                    if (isExpanded) CommonStrings.collapse_icon
                    else CommonStrings.expand_icon,
                ),
            )
        }
        AnimatedVisibility(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            visible = isExpanded,
            enter = EnterTransition,
            exit = ExitTransition,
            content = {
                Column(
                    modifier = Modifier
                        // Inset expanded content from the start to disambiguate
                        // it from other items
                        .padding(start = 8.dp)
                        .fillMaxWidth(),
                ) {
                    content()
                }
            },
        )
    }
}

@Composable
fun SettingsToggleItem(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = SettingsItemClipModifier
            .then(
                modifier
                    .toggleable(
                        value = checked,
                        onValueChange = onCheckedChange,
                        enabled = enabled,
                        role = Role.Switch,
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    ),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = text,
        )
        Spacer(
            modifier = Modifier
                .width(16.dp),
        )
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
fun NotificationStatusText(
    preference: NotificationPreferences.Preference.Filterable,
) {
    val off = stringResource(Res.string.off)
    val inApp = stringResource(Res.string.in_app)
    val push = stringResource(Res.string.push)
    val everyone = stringResource(Res.string.everyone)
    val peopleYouFollow = stringResource(Res.string.people_you_follow)

    val statusText = remember(preference) {
        buildString {
            if (!preference.list && !preference.push) {
                append(off)
            } else {
                if (preference.list) append(inApp)
                if (preference.list && preference.push) append(", ")
                if (preference.push) append(push)

                append(" • ")

                append(
                    when (preference.include) {
                        NotificationPreferences.Include.All -> everyone
                        NotificationPreferences.Include.Follows -> peopleYouFollow
                        else -> ""
                    },
                )
            }
        }
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun CombinedNotificationStatusText(
    item: NotificationSettingItem,
) {
    val statusRes = when (item) {
        is NotificationSettingItem.EverythingElse -> {
            val allPushEnabled = item.preferences.all { it.push }
            if (allPushEnabled) Res.string.push else Res.string.off
        }

        is NotificationSettingItem.ActivityFromOthers -> {
            val hasInApp = item.preferences.firstOrNull()?.list ?: false
            val hasPush = item.preferences.firstOrNull()?.push ?: false

            when {
                !hasInApp && !hasPush -> Res.string.off
                hasInApp && hasPush -> Res.string.push_and_in_app
                hasInApp -> Res.string.in_app
                hasPush -> Res.string.push
                else -> Res.string.off
            }
        }

        else -> return
    }

    Text(
        text = stringResource(statusRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val EnterTransition = fadeIn() + slideInVertically { -it }
private val ExitTransition =
    shrinkOut { IntSize(it.width, 0) } + slideOutVertically { -it } + fadeOut()

private val SettingsItemShape = RoundedCornerShape(8.dp)
private val SettingsItemClipModifier = Modifier
    .clip(SettingsItemShape)
