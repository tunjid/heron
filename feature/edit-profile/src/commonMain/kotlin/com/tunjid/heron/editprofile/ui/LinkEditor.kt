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

package com.tunjid.heron.editprofile.ui

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.ProfileLinks
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.editprofile.EditableLink
import com.tunjid.heron.editprofile.ui.ProfileLinkSheetState.Companion.rememberProfileLinkSheetState
import com.tunjid.heron.profile.ProfileLinkIcon
import com.tunjid.heron.ui.JiggleBox
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState
import com.tunjid.heron.ui.draganddrop.DragAndDropSelectorState.Companion.selectorDragAndDrop
import com.tunjid.heron.ui.fillMaxRestrictedWidth
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.Add
import com.tunjid.heron.ui.icons.regular.Remove
import com.tunjid.heron.ui.modifiers.chipBackground
import heron.feature.edit_profile.generated.resources.Res
import heron.feature.edit_profile.generated.resources.link_add
import heron.feature.edit_profile.generated.resources.link_limit_reached
import heron.feature.edit_profile.generated.resources.link_remove
import heron.feature.edit_profile.generated.resources.links_description
import heron.feature.edit_profile.generated.resources.links_empty
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun LinkEditor(
    modifier: Modifier = Modifier,
    links: List<EditableLink>,
    onLinksChanged: (List<EditableLink>) -> Unit,
) {
    val sheetState = rememberProfileLinkSheetState(
        onLinkSaved = { original, saved ->
            onLinksChanged(
                if (original == null) links + saved
                else links.map { if (it == original) saved else it },
            )
        },
        onLinkRemoved = { removed ->
            onLinksChanged(links - removed)
        },
    )
    val dragAndDropState = remember(links) {
        DragAndDropSelectorState(
            items = links.toMutableStateList(),
            id = EditableLink::id,
            selected = { true },
        )
    }
    val updatedOnLinksChanged by rememberUpdatedState(onLinksChanged)
    LaunchedEffect(dragAndDropState) {
        snapshotFlow { dragAndDropState.items.toList() }
            .filter { it != links }
            .collect { updatedOnLinksChanged(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(
                if (links.isEmpty()) Res.string.links_empty
                else Res.string.links_description,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LookaheadScope {
            FlowRow(
                modifier = Modifier
                    .fillMaxRestrictedWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dragAndDropState.items.forEach { link ->
                    key(link.id) {
                        if (!dragAndDropState.isDraggedId(link.id)) LinkChip(
                            modifier = Modifier
                                .animateBounds(lookaheadScope = this@LookaheadScope),
                            dragAndDropState = dragAndDropState,
                            link = link,
                            onClick = {
                                sheetState.editLink(
                                    link = link,
                                    otherUrls = links.mapNotNull { other ->
                                        other.url.takeIf { other != link }
                                    },
                                )
                            },
                            onRemove = {
                                onLinksChanged(links - link)
                            },
                        )
                    }
                }
                if (links.size < ProfileLinks.MAX_LINKS) key(Res.string.link_add) {
                    AssistChip(
                        modifier = Modifier
                            .animateBounds(
                                lookaheadScope = this@LookaheadScope,
                            ),
                        shape = CircleShape,
                        onClick = {
                            sheetState.addLink(
                                existingUrls = links.map(EditableLink::url),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = HeronIcons.Regular.Add,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(text = stringResource(Res.string.link_add))
                        },
                    )
                }
            }
        }
        if (links.size >= ProfileLinks.MAX_LINKS) Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = stringResource(
                Res.string.link_limit_reached,
                ProfileLinks.MAX_LINKS,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LinkChip(
    modifier: Modifier = Modifier,
    dragAndDropState: DragAndDropSelectorState<EditableLink>,
    link: EditableLink,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    JiggleBox(
        modifier = modifier,
    ) {
        InputChip(
            modifier = Modifier
                .chipBackground(
                    animateColorAsState(
                        if (dragAndDropState.isHoveredId(link.id)) TabsState.TabBackgroundColor
                        else Color.Transparent,
                    )::value,
                )
                .selectorDragAndDrop(
                    state = dragAndDropState,
                    id = link.id,
                ),
            shape = CircleShape,
            selected = false,
            onClick = onClick,
            leadingIcon = {
                ProfileLinkIcon(
                    modifier = Modifier
                        .size(LinkIconSize),
                    url = link.url,
                    favicon = link.icon,
                )
            },
            label = {
                Text(
                    modifier = Modifier
                        .width(IntrinsicSize.Max),
                    text = link.displayText,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                Icon(
                    modifier = Modifier
                        .clickable(onClick = onRemove),
                    imageVector = HeronIcons.Regular.Remove,
                    contentDescription = stringResource(Res.string.link_remove),
                )
            },
        )
    }
}

private val EditableLink.id: String
    get() = url.uri

private val EditableLink.displayText: String
    get() = title ?: runCatching { url.domain }.getOrDefault(url.uri)

private val LinkIconSize = 18.dp
