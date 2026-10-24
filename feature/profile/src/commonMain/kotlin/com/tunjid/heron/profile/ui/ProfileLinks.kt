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

package com.tunjid.heron.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.AppliedLabels
import com.tunjid.heron.data.core.models.AppliedLabels.Companion.shouldHideProfileLink
import com.tunjid.heron.data.core.models.AppliedLabels.Companion.withLabels
import com.tunjid.heron.data.core.models.ProfileLink
import com.tunjid.heron.data.core.types.Uri
import com.tunjid.heron.data.core.types.domain
import com.tunjid.heron.data.core.types.takeIfIs
import com.tunjid.heron.profile.ProfileLinkIcon
import com.tunjid.heron.ui.icons.HeronIcons
import com.tunjid.heron.ui.icons.regular.ArrowOutward

@Composable
fun ProfileLinks(
    modifier: Modifier = Modifier,
    links: List<ProfileLink>,
    appliedLabels: AppliedLabels,
) {
    val visibleLinks = remember(
        links,
        appliedLabels,
    ) {
        links.filterNot { link ->
            appliedLabels.withLabels(link.labels).shouldHideProfileLink()
        }
    }
    // Emit nothing, so an empty row doesn't add spacing to the header
    if (visibleLinks.isEmpty()) return

    val uriHandler = LocalUriHandler.current
    // A single scrolling row, so a profile with many links can't push the rest of
    // the header down
    LazyRow(
        modifier = modifier
            .clip(LinksRowShape),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = visibleLinks,
            key = { link -> link.uri.uri },
        ) { link ->
            ElevatedAssistChip(
                shape = CircleShape,
                onClick = {
                    runCatching {
                        link.url.uri
                            .takeIfIs(Uri.Host.Https)
                            ?.let(uriHandler::openUri)
                    }
                },
                leadingIcon = {
                    ProfileLinkIcon(
                        modifier = Modifier
                            .size(ProfileLinkIconSize),
                        url = link.url,
                        favicon = link.icon,
                    )
                },
                label = {
                    Text(
                        text = link.title ?: link.domainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                // Shows the chip leaves the app
                trailingIcon = {
                    Icon(
                        modifier = Modifier
                            .size(LinkOutIconSize),
                        imageVector = HeronIcons.Regular.ArrowOutward,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

private val ProfileLink.domainText: String
    get() = runCatching { url.domain }.getOrDefault(url.uri)

private val LinksRowShape = RoundedCornerShape(percent = 50)

private val ProfileLinkIconSize = 18.dp
private val LinkOutIconSize = 14.dp
