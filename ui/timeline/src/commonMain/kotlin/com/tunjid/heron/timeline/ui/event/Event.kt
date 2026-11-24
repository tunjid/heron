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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.PolygonShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.Event
import com.tunjid.heron.data.core.models.Rsvp
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.media.images.AsyncImage
import com.tunjid.heron.media.images.ImageArgs
import com.tunjid.heron.media.images.ImageRequest
import com.tunjid.heron.timeline.utilities.BokehPainter
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.formatHour
import com.tunjid.heron.timeline.utilities.formatWeekdayDate
import com.tunjid.heron.timeline.utilities.sensitiveContentBlur
import com.tunjid.heron.ui.Chip
import com.tunjid.heron.ui.LatchedState
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.CalendarMonth
import com.tunjid.heron.ui.icons.regular.Cancel
import com.tunjid.heron.ui.icons.regular.Check
import com.tunjid.heron.ui.icons.regular.CheckCircle
import com.tunjid.heron.ui.icons.regular.Delete
import com.tunjid.heron.ui.icons.regular.ExpandMore
import com.tunjid.heron.ui.icons.regular.Group
import com.tunjid.heron.ui.icons.regular.Groups
import com.tunjid.heron.ui.icons.regular.JoinFull
import com.tunjid.heron.ui.icons.regular.Link
import com.tunjid.heron.ui.icons.regular.LocationOn
import com.tunjid.heron.ui.icons.regular.Star
import com.tunjid.heron.ui.icons.regular.Timer
import com.tunjid.heron.ui.icons.regular.Videocam
import com.tunjid.heron.ui.icons.regular.Warning
import com.tunjid.heron.ui.rememberLatchedState
import com.tunjid.heron.ui.shapes.roundedRectangle
import com.tunjid.heron.ui.titleSharedElementKey
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.event_ends_at
import heron.ui.timeline.generated.resources.event_mode_hybrid
import heron.ui.timeline.generated.resources.event_mode_in_person
import heron.ui.timeline.generated.resources.event_mode_virtual
import heron.ui.timeline.generated.resources.event_rsvp_counts
import heron.ui.timeline.generated.resources.event_rsvp_going
import heron.ui.timeline.generated.resources.event_rsvp_interested
import heron.ui.timeline.generated.resources.event_rsvp_not_going
import heron.ui.timeline.generated.resources.event_rsvp_remove
import heron.ui.timeline.generated.resources.event_status_cancelled
import heron.ui.timeline.generated.resources.event_status_postponed
import heron.ui.timeline.generated.resources.event_status_rescheduled
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Event(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    event: Event,
    isBlurred: Boolean,
    onRsvpClicked: (Event, Rsvp.Status?) -> Unit,
) = with(paneTransitionScope) {
    val currentStatus = event.rsvp?.status
    val latchedStatus = rememberLatchedState(currentStatus)
    val canRsvp = event.cid != null && event.status != Event.Status.Cancelled

    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(8.dp),
    ) {
        EventInfo(
            isBlurred = isBlurred,
            event = event,
            sharedElementPrefix = sharedElementPrefix,
            latchedStatus = latchedStatus,
            currentStatus = currentStatus,
        )
        event.locationText()?.let {
            EventSharedElement(
                key = event.sharedElementKey(
                    prefix = sharedElementPrefix,
                    part = "location",
                ),
            ) {
                EventInfoRow(
                    icon = HeronIcons.Regular.LocationOn,
                    text = it,
                )
            }
        }
        EventSharedElement(
            key = event.sharedElementKey(
                prefix = sharedElementPrefix,
                part = "rsvp",
            ),
        ) {
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
}

@Composable
private fun PaneTransitionScope.EventSharedElement(
    key: String,
    content: @Composable () -> Unit,
) {
    PaneStickySharedElement(
        boundsTransform = sharedElementBoundsTransform,
        clipInOverlayDuringTransition = paneOverlayClip,
        sharedContentState = rememberSharedContentState(
            key = key,
        ),
    ) {
        content()
    }
}

/**
 * Details of an [Event] beyond what its card shows: when it ends, how it's held,
 * every link to join or learn more, and any further locations.
 */
@Composable
fun EventDetails(
    modifier: Modifier = Modifier,
    event: Event,
) {
    val uriHandler = LocalUriHandler.current
    // The card shows the first location, so only the rest are listed here.
    val shownLocation = event.places.locations.firstOrNull { it.displayText() != null }
    val links = event.places.locations
        .filterIsInstance<Event.Location.Online>()
        .map { it.uri to (it.name.nonBlank() ?: it.uri.domain) } +
        event.places.links.map { it.uri to (it.name.nonBlank() ?: it.uri.domain) }
    val otherLocations = event.places.locations
        .filterNot { it is Event.Location.Online || it == shownLocation }
        .mapNotNull(Event.Location::displayText)

    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(8.dp),
    ) {
        event.endsAt?.let { endsAt ->
            EventInfoRow(
                icon = HeronIcons.Regular.Timer,
                text = stringResource(
                    Res.string.event_ends_at,
                    "${endsAt.formatWeekdayDate()}, ${endsAt.formatHour()}",
                ),
            )
        }
        event.mode.icon?.let { modeIcon ->
            event.mode.label?.let { modeLabel ->
                EventInfoRow(
                    icon = modeIcon,
                    text = stringResource(modeLabel),
                )
            }
        }
        otherLocations.forEach { location ->
            EventInfoRow(
                icon = HeronIcons.Regular.LocationOn,
                text = location,
            )
        }
        links.forEach { (uri, label) ->
            EventInfoRow(
                modifier = Modifier
                    .clickable {
                        runCatching { uriHandler.openUri(uri.uri) }
                    },
                icon = HeronIcons.Regular.Link,
                text = label,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PaneTransitionScope.EventInfo(
    isBlurred: Boolean,
    event: Event,
    sharedElementPrefix: String,
    latchedStatus: LatchedState<Rsvp.Status?>,
    currentStatus: Rsvp.Status?,
) {
    Row(
        horizontalArrangement = spacedBy(12.dp),
    ) {
        PaneStickySharedElement(
            boundsTransform = sharedElementBoundsTransform,
            clipInOverlayDuringTransition = paneOverlayClip,
            modifier = Modifier
                .size(88.dp)
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
                args = remember(
                    event.thumbnail,
                    event.uri,
                    event.name,
                ) {
                    ImageArgs(
                        request = when (val thumbnail = event.thumbnail) {
                            null -> ImageRequest.Painted(
                                painter = BokehPainter(
                                    seed = event.uri.uri.hashCode(),
                                ),
                            )
                            else -> ImageRequest.Network(
                                url = thumbnail.uri,
                            )
                        },
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
            EventSharedElement(
                key = titleSharedElementKey(
                    prefix = sharedElementPrefix,
                    type = event.uri,
                ),
            ) {
                Text(
                    text = event.name,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            event.startDateTime()?.let { startDateTime ->
                EventSharedElement(
                    key = event.sharedElementKey(
                        prefix = sharedElementPrefix,
                        part = "date",
                    ),
                ) {
                    EventInfoRow(
                        icon = HeronIcons.Regular.CalendarMonth,
                        text = startDateTime,
                    )
                }
            }
            event.status.label?.let { statusLabel ->
                EventSharedElement(
                    key = event.sharedElementKey(
                        prefix = sharedElementPrefix,
                        part = "status",
                    ),
                ) {
                    EventInfoRow(
                        icon = HeronIcons.Regular.Warning,
                        text = stringResource(statusLabel),
                        color = when (event.status) {
                            Event.Status.Cancelled -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
            }
            EventSharedElement(
                key = event.sharedElementKey(
                    prefix = sharedElementPrefix,
                    part = "counts",
                ),
            ) {
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
}

@Composable
private fun EventInfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.outline,
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
            tint = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
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
            horizontalArrangement = spacedBy(8.dp),
        ) {
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
                    Chip(
                        selected = isSelected,
                        text = stringResource(option.label),
                        enabled = enabled,
                        leadingIcon = option.icon,
                        trailingIcon =
                        if (isSelected) HeronIcons.Regular.ExpandMore
                        else null,
                        onClick = {
                            if (isSelected) expanded = true
                            else onStatusSelected(status)
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
    places.locations.firstNotNullOfOrNull(Event.Location::displayText)

private fun Event.Location.displayText(): String? =
    name.nonBlank() ?: when (this) {
        is Event.Location.Address -> listOfNotNull(
            street,
            locality,
            region,
            country,
        ).joinToString(separator = ", ")
        is Event.Location.Online -> uri.domain
        is Event.Location.Geo -> "$latitude, $longitude"
        is Event.Location.Foursquare,
        is Event.Location.H3,
        -> null
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

private val Event.Mode.label: StringResource?
    get() = when (this) {
        Event.Mode.InPerson -> Res.string.event_mode_in_person
        Event.Mode.Virtual -> Res.string.event_mode_virtual
        Event.Mode.Hybrid -> Res.string.event_mode_hybrid
        Event.Mode.Unknown -> null
    }

private fun Event.sharedElementKey(
    prefix: String,
    part: String,
): String = "$prefix-${uri.uri}-$part"

private val Event.Mode.icon: ImageVector?
    get() = when (this) {
        Event.Mode.InPerson -> HeronIcons.Regular.Groups
        Event.Mode.Virtual -> HeronIcons.Regular.Videocam
        Event.Mode.Hybrid -> HeronIcons.Regular.JoinFull
        Event.Mode.Unknown -> null
    }

/**
 * Only statuses that change what attendees should expect are called out.
 */
private val Event.Status.label: StringResource?
    get() = when (this) {
        Event.Status.Cancelled -> Res.string.event_status_cancelled
        Event.Status.Postponed -> Res.string.event_status_postponed
        Event.Status.Rescheduled -> Res.string.event_status_rescheduled
        Event.Status.Planned,
        Event.Status.Scheduled,
        Event.Status.Unknown,
        -> null
    }

// Records may carry empty names, which should fall back like missing ones.
private fun String?.nonBlank(): String? = this?.takeIf(String::isNotBlank)

private val EventImageShape = PolygonShape.roundedRectangle(percent = 0.25f)
