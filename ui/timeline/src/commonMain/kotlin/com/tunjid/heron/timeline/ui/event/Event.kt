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

package com.tunjid.heron.timeline.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.types.ImageUri
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.formatHour
import com.tunjid.heron.timeline.utilities.formatWeekdayDate
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.LatchedState
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.TabsState
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.CalendarMonth
import com.tunjid.heron.ui.icons.regular.Cancel
import com.tunjid.heron.ui.icons.regular.Check
import com.tunjid.heron.ui.icons.regular.CheckCircle
import com.tunjid.heron.ui.icons.regular.Delete
import com.tunjid.heron.ui.icons.regular.ExpandMore
import com.tunjid.heron.ui.icons.regular.Group
import com.tunjid.heron.ui.icons.regular.LocationOn
import com.tunjid.heron.ui.icons.regular.Star
import com.tunjid.heron.ui.rememberLatchedState
import com.tunjid.heron.ui.shapes.roundedRectangle
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.event_mode_hybrid
import heron.ui.timeline.generated.resources.event_mode_in_person
import heron.ui.timeline.generated.resources.event_mode_virtual
import heron.ui.timeline.generated.resources.event_rsvp_counts
import heron.ui.timeline.generated.resources.event_rsvp_going
import heron.ui.timeline.generated.resources.event_rsvp_interested
import heron.ui.timeline.generated.resources.event_rsvp_not_going
import heron.ui.timeline.generated.resources.event_rsvp_remove
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Event(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    event: Event,
    image: ImageUri?,
    isBlurred: Boolean,
    onRsvpClicked: (Event, Rsvp.Status?) -> Unit,
) = with(paneTransitionScope) {
    val currentStatus = event.rsvp?.status
    val latchedStatus = rememberLatchedState(currentStatus)
    val canRsvp = event.cid != null

    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(8.dp),
    ) {
        EventInfo(
            image = event.thumbnail ?: image,
            imageAspectRatio = event.thumbnailAspectRatio
                ?.takeIf { event.thumbnail != null }
                ?: 1f,
            isBlurred = isBlurred,
            event = event,
            sharedElementPrefix = sharedElementPrefix,
            latchedStatus = latchedStatus,
            currentStatus = currentStatus,
        )
        event.locationText()?.let {
            EventInfoRow(
                icon = HeronIcons.Regular.LocationOn,
                text = it,
            )
        }
        RsvpRow(
            answeredStatus = latchedStatus.value
                ?.takeIf(RsvpOptions::containsKey),
            enabled = canRsvp,
            onStatusSelected = { status ->
                if (latchedStatus.isCurrent) {
                    latchedStatus.latch(status)
                    onRsvpClicked(event, status)
                }
            },
        )
    }
}

@Composable
private fun PaneTransitionScope.EventInfo(
    image: ImageUri?,
    imageAspectRatio: Float,
    isBlurred: Boolean,
    event: Event,
    sharedElementPrefix: String,
    latchedStatus: LatchedState<Rsvp.Status?>,
    currentStatus: Rsvp.Status?,
) {
    Row(
        horizontalArrangement = spacedBy(12.dp),
    ) {
        if (image != null) PaneStickySharedElement(
            boundsTransform = sharedElementBoundsTransform,
            clipInOverlayDuringTransition = paneOverlayClip,
            modifier = Modifier
                .height(88.dp)
                .aspectRatio(imageAspectRatio)
                .then(
                    if (isBlurred) Modifier.sensitiveContentBlur(EventImageShape)
                    else Modifier,
                ),
            sharedContentState = rememberSharedContentState(
                key = event.avatarSharedElementKey(
                    prefix = sharedElementPrefix,
                ),
            ),
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                args = remember(image, event.name) {
                    ImageArgs(
                        url = image.uri,
                        contentDescription = event.name,
                        contentScale = ContentScale.Crop,
                        shape = EventImageShape,
                    )
                },
            )
        }
        Column(
            verticalArrangement = spacedBy(4.dp),
        ) {
            Text(
                text = event.name,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            event.startDateTime()?.let { startDateTime ->
                EventInfoRow(
                    icon = HeronIcons.Regular.CalendarMonth,
                    text = startDateTime,
                )
            }
            EventInfoRow(
                icon = HeronIcons.Regular.Group,
                text = stringResource(
                    Res.string.event_rsvp_counts,
                    event.goingCount + latchedStatus.value.countDelta(
                        currentStatus = currentStatus,
                        status = Rsvp.Status.Going,
                    ),
                    event.interestedCount + latchedStatus.value.countDelta(
                        currentStatus = currentStatus,
                        status = Rsvp.Status.Interested,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun EventInfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(18.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RsvpRow(
    answeredStatus: Rsvp.Status?,
    enabled: Boolean,
    onStatusSelected: (Rsvp.Status?) -> Unit,
) {
    val visibleStatuses = when (answeredStatus) {
        null -> UnansweredRsvpStatuses
        else -> listOf(answeredStatus)
    }
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // A fixed first item anchors the scroll position, so chips inserted
            // ahead of the visible ones animate in instead of shifting the row.
            item(
                key = RsvpRowAnchorKey,
            ) {
                Spacer(
                    modifier = Modifier
                        .width(0.dp),
                )
            }
            items(
                items = visibleStatuses,
                key = Rsvp.Status::id,
            ) { status ->
                val option = RsvpOptions.getValue(status)
                val isSelected = status == answeredStatus
                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .animateItem(),
                ) {
                    FilterChip(
                        shape = RsvpChipShape,
                        border = null,
                        selected = isSelected,
                        enabled = enabled,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainerColor = TabsState.TabBackgroundColor,
                            iconColor = MaterialTheme.colorScheme.outline,
                            labelColor = MaterialTheme.colorScheme.outline,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        onClick = {
                            if (isSelected) expanded = true
                            else onStatusSelected(status)
                        },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize),
                                imageVector = option.icon,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (isSelected) {
                            {
                                Icon(
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                    imageVector = HeronIcons.Regular.ExpandMore,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                        label = {
                            Text(
                                text = stringResource(option.label),
                                maxLines = 1,
                            )
                        },
                    )
                    RsvpMenu(
                        expanded = expanded && isSelected,
                        status = status,
                        onDismissRequest = {
                            expanded = false
                        },
                        onStatusSelected = { selectedStatus ->
                            expanded = false
                            onStatusSelected(selectedStatus)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RsvpMenu(
    expanded: Boolean,
    status: Rsvp.Status,
    onDismissRequest: () -> Unit,
    onStatusSelected: (Rsvp.Status?) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        RsvpOptions.forEach { (itemStatus, itemOption) ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(itemOption.label),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = itemOption.icon,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (itemStatus == status) Icon(
                        imageVector = HeronIcons.Regular.Check,
                        contentDescription = null,
                    )
                },
                onClick = {
                    if (itemStatus == status) onDismissRequest()
                    else onStatusSelected(itemStatus)
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(Res.string.event_rsvp_remove),
                    color = MaterialTheme.colorScheme.error,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = HeronIcons.Regular.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                onStatusSelected(null)
            },
        )
    }
}

private fun Rsvp.Status?.countDelta(
    currentStatus: Rsvp.Status?,
    status: Rsvp.Status,
): Long = (if (this == status) 1L else 0L) - (if (currentStatus == status) 1L else 0L)

@Composable
private fun Event.startDateTime(): String? {
    val start = startsAt ?: return null
    return "${start.formatWeekdayDate()}, ${start.formatHour()}"
}

private fun Event.locationText(): String? =
    places.locations.firstNotNullOfOrNull { location ->
        location.name ?: when (location) {
            is Event.Location.Address -> listOfNotNull(
                location.street,
                location.locality,
                location.region,
                location.country,
            ).joinToString(separator = ", ")
            is Event.Location.Online -> location.uri.domain
            is Event.Location.Geo -> "${location.latitude}, ${location.longitude}"
            is Event.Location.Foursquare,
            is Event.Location.H3,
            -> null
        }
    }

private class RsvpOption(
    val icon: ImageVector,
    val label: StringResource,
)

private val RsvpOptions = mapOf(
    Rsvp.Status.Going to RsvpOption(
        icon = HeronIcons.Regular.CheckCircle,
        label = Res.string.event_rsvp_going,
    ),
    Rsvp.Status.Interested to RsvpOption(
        icon = HeronIcons.Regular.Star,
        label = Res.string.event_rsvp_interested,
    ),
    Rsvp.Status.NotGoing to RsvpOption(
        icon = HeronIcons.Regular.Cancel,
        label = Res.string.event_rsvp_not_going,
    ),
)

private val UnansweredRsvpStatuses = listOf(
    Rsvp.Status.Going,
    Rsvp.Status.Interested,
)

private const val RsvpRowAnchorKey = "anchor"

private val RsvpChipShape = RoundedCornerShape(16.dp)

@Suppress("unused")
private val Event.Mode.label: StringResource?
    get() = when (this) {
        Event.Mode.InPerson -> Res.string.event_mode_in_person
        Event.Mode.Virtual -> Res.string.event_mode_virtual
        Event.Mode.Hybrid -> Res.string.event_mode_hybrid
        Event.Mode.Unknown -> null
    }

private val EventImageShape = PolygonShape.roundedRectangle(percent = 0.25f)
