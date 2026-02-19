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

package com.tunjid.heron.notificationsettings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.NotificationPreferences
import com.tunjid.heron.notificationsettings.ui.CombinedNotificationStatusText
import com.tunjid.heron.notificationsettings.ui.ExpandableSettingsItemRow
import com.tunjid.heron.notificationsettings.ui.NotificationSettingItem
import com.tunjid.heron.notificationsettings.ui.NotificationSettingsRadioButton
import com.tunjid.heron.notificationsettings.ui.NotificationStatusText
import com.tunjid.heron.notificationsettings.ui.SettingsToggleItem
import com.tunjid.heron.notificationsettings.ui.toNotificationSettingItems
import com.tunjid.heron.scaffold.scaffold.PaneScaffoldState
import com.tunjid.heron.ui.UiTokens
import heron.feature.notification_settings.generated.resources.Res
import heron.feature.notification_settings.generated.resources.everyone
import heron.feature.notification_settings.generated.resources.from
import heron.feature.notification_settings.generated.resources.in_app_notifications
import heron.feature.notification_settings.generated.resources.people_i_follow
import heron.feature.notification_settings.generated.resources.push_notifications
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationSettingsScreen(
    paneScaffoldState: PaneScaffoldState,
    state: State,
    actions: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items =
        remember(state.notificationPreferences, state.pendingUpdates) {
            state.notificationPreferences?.toNotificationSettingItems(
                pendingUpdates = state.pendingUpdates
            ) ?: emptyList()
        }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            UiTokens.bottomNavAndInsetPaddingValues(
                horizontal = 16.dp,
                isCompact = paneScaffoldState.prefersCompactBottomNav,
            ),
    ) {
        items(
            items = items,
            key = { item ->
                when (item) {
                    is NotificationSettingItem.Filterable -> "filterable_${item.reason.name}"
                    is NotificationSettingItem.EverythingElse -> "everything_else"
                    is NotificationSettingItem.ActivityFromOthers -> "activity_from_others"
                }
            },
        ) { item ->
            NotificationSettingRow(
                modifier = Modifier.animateItem(),
                item = item,
                onUpdate = { update -> actions(Action.CacheNotificationPreferenceUpdate(update)) },
            )
        }
    }
}

@Composable
fun NotificationSettingRow(
    modifier: Modifier = Modifier,
    item: NotificationSettingItem,
    onUpdate: (NotificationPreferences.Update) -> Unit,
) {
    when (item) {
        is NotificationSettingItem.Filterable ->
            FilterableNotificationSetting(modifier = modifier, item = item, onUpdate = onUpdate)
        is NotificationSettingItem.Combined ->
            CombinedNotificationSetting(
                modifier = modifier,
                combinedItem = item,
                onUpdate = onUpdate,
            )
    }
}

@Composable
private fun FilterableNotificationSetting(
    modifier: Modifier = Modifier,
    item: NotificationSettingItem.Filterable,
    onUpdate: (NotificationPreferences.Update) -> Unit,
) {
    ExpandableSettingsItemRow(
        modifier = modifier,
        title = stringResource(item.title),
        icon = item.icon,
        status = { NotificationStatusText(item.preference) },
        content = {
            Text(
                text = stringResource(item.description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SettingsToggleItem(
                text = stringResource(Res.string.in_app_notifications),
                enabled = true,
                checked = item.preference.list,
                onCheckedChange = { checked ->
                    val update =
                        NotificationPreferences.Update(
                            reason = item.reason,
                            list = checked,
                            push = item.preference.push,
                            include = item.preference.include,
                        )
                    onUpdate(update)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleItem(
                text = stringResource(Res.string.push_notifications),
                enabled = true,
                checked = item.preference.push,
                onCheckedChange = { checked ->
                    val update =
                        NotificationPreferences.Update(
                            reason = item.reason,
                            list = item.preference.list,
                            push = checked,
                            include = item.preference.include,
                        )
                    onUpdate(update)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.from),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            NotificationSettingsRadioButton(
                text = stringResource(Res.string.everyone),
                selected = item.preference.include == NotificationPreferences.Include.All,
                onSelect = {
                    val update =
                        NotificationPreferences.Update(
                            reason = item.reason,
                            list = item.preference.list,
                            push = item.preference.push,
                            include = NotificationPreferences.Include.All,
                        )
                    onUpdate(update)
                },
            )

            NotificationSettingsRadioButton(
                text = stringResource(Res.string.people_i_follow),
                selected = item.preference.include == NotificationPreferences.Include.Follows,
                onSelect = {
                    val update =
                        NotificationPreferences.Update(
                            reason = item.reason,
                            list = item.preference.list,
                            push = item.preference.push,
                            include = NotificationPreferences.Include.Follows,
                        )
                    onUpdate(update)
                },
            )
        },
    )
}

@Composable
private fun CombinedNotificationSetting(
    modifier: Modifier = Modifier,
    combinedItem: NotificationSettingItem.Combined,
    onUpdate: (NotificationPreferences.Update) -> Unit,
) {
    val title = stringResource(combinedItem.title)
    val icon = combinedItem.icon
    val description = stringResource(combinedItem.description)

    ExpandableSettingsItemRow(
        modifier = modifier,
        title = title,
        icon = icon,
        status = { CombinedNotificationStatusText(item = combinedItem) },
        content = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (combinedItem is NotificationSettingItem.EverythingElse) {
                val anyPushEnabled = combinedItem.preferences.any { it.value.push }
                SettingsToggleItem(
                    text = stringResource(Res.string.push_notifications),
                    enabled = true,
                    checked = anyPushEnabled,
                    onCheckedChange = { checked ->
                        combinedItem.preferences.forEach { (reason, pref) ->
                            onUpdate(
                                NotificationPreferences.Update(
                                    reason = reason,
                                    list = pref.list,
                                    push = checked,
                                    include = null,
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (combinedItem is NotificationSettingItem.ActivityFromOthers) {
                SettingsToggleItem(
                    text = stringResource(Res.string.in_app_notifications),
                    enabled = true,
                    checked = combinedItem.preferences.any { it.value.list },
                    onCheckedChange = { checked ->
                        combinedItem.preferences.forEach { (reason, pref) ->
                            onUpdate(
                                NotificationPreferences.Update(
                                    reason = reason,
                                    list = checked,
                                    push = pref.push,
                                    include = null,
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleItem(
                    text = stringResource(Res.string.push_notifications),
                    enabled = true,
                    checked = combinedItem.preferences.any { it.value.push },
                    onCheckedChange = { checked ->
                        combinedItem.preferences.forEach { (reason, pref) ->
                            onUpdate(
                                NotificationPreferences.Update(
                                    reason = reason,
                                    list = pref.list,
                                    push = checked,
                                    include = null,
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
