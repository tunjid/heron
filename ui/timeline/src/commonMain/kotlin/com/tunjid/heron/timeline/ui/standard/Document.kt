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

package com.tunjid.heron.timeline.ui.standard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.StandardDocument
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.DocumentCollectionShape
import com.tunjid.heron.timeline.utilities.Label
import com.tunjid.heron.timeline.utilities.LabelFlowRow
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.ui.AttributionLayout
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.RecordBlurb
import com.tunjid.heron.ui.RecordSubtitle
import com.tunjid.heron.ui.RecordText
import com.tunjid.heron.ui.RecordTitle
import com.tunjid.heron.ui.rememberLatchedState
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.standard_publication_subscribe_to
import heron.ui.core.generated.resources.standard_publication_unsubscribe_from
import heron.ui.timeline.generated.resources.Res
import heron.ui.timeline.generated.resources.standard_site_published_in
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
fun Document(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String,
    document: StandardDocument,
    onPublicationClicked: ((StandardPublication) -> Unit)?,
    onSubscriptionToggled: ((StandardPublication, StandardSubscription?) -> Unit)?,
) = with(paneTransitionScope) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AttributionLayout(
            modifier = Modifier
                .fillMaxWidth(),
            avatar = {
                document.coverImage?.let { cover ->
                    AsyncImage(
                        modifier = Modifier
                            .size(48.dp),
                        args = remember(
                            cover,
                        ) {
                            ImageArgs(
                                url = cover.uri,
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                shape = DocumentCollectionShape,
                            )
                        },
                    )
                }
            },
            label = {
                RecordTitle(
                    title = document.title,
                )
                Spacer(Modifier.height(2.dp))

                if (onPublicationClicked != null) document.publication?.let { publication ->
                    val publisherDescription = stringResource(
                        Res.string.standard_site_published_in,
                        publication.name,
                    )
                    Label(
                        modifier = Modifier
                            .padding(
                                vertical = 2.dp,
                            ),
                        contentDescription = publisherDescription,
                        icon = {
                            publication.icon?.let { icon ->
                                PaneStickySharedElement(
                                    modifier = Modifier
                                        .size(20.dp),
                                    sharedContentState = rememberSharedContentState(
                                        key = publication.avatarSharedElementKey(
                                            prefix = sharedElementPrefix,
                                            creator = StandardPublication::publisher,
                                        ),
                                    ),
                                ) {
                                    AsyncImage(
                                        modifier = Modifier
                                            .fillParentAxisIfFixedOrWrap(),
                                        args = remember(
                                            icon,
                                        ) {
                                            ImageArgs(
                                                url = icon.uri,
                                                contentScale = ContentScale.Crop,
                                                contentDescription = null,
                                                shape = DocumentCollectionShape,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                        description = {
                            RecordSubtitle(
                                subtitle = publisherDescription,
                            )
                        },
                        onClick = {
                            onPublicationClicked(publication)
                        },
                    )
                }
            },
        )

        document.description.takeUnless(String?::isNullOrEmpty)?.let {
            RecordText(
                text = it,
            )
        }
        LabelFlowRow {
            RecordBlurb(
                remember(document.publishedAt) {
                    document.publishDate()
                },
            )

            document.tags
                .take(3)
                .forEach {
                    Label(
                        contentDescription = it,
                        isElevated = true,
                        icon = {},
                        description = {
                            RecordBlurb(it)
                        },
                        onClick = {},
                    )
                }

            if (onSubscriptionToggled != null) SubscribeButton(
                document = document,
                onSubscriptionToggled = onSubscriptionToggled,
            )
        }
    }
}

@Composable
private fun SubscribeButton(
    document: StandardDocument,
    onSubscriptionToggled: (StandardPublication, StandardSubscription?) -> Unit,
) {
    document.publication?.let { publication ->
        val subscribed = publication.subscription != null
        val latchedSubscribedState = rememberLatchedState(subscribed)

        Label(
            contentDescription = stringResource(
                if (latchedSubscribedState.value) CommonStrings.standard_publication_unsubscribe_from
                else CommonStrings.standard_publication_subscribe_to,
            ),
            isElevated = true,
            icon = {
                PublicationSubscriptionIcon(
                    subscribed = latchedSubscribedState.value,
                    iconSize = 16.dp,
                    iconTint = MaterialTheme.colorScheme.outline,
                )
            },
            description = {},
            onClick = {
                latchedSubscribedState.latch(!subscribed)
                onSubscriptionToggled(
                    publication,
                    publication.subscription,
                )
            },
        )
    }
}

private fun StandardDocument.publishDate(): String =
    publishedAt
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(
            LocalDateTime.Format {
                monthNumber()
                char('.')
                day()
                char('.')
                year()
            },
        )
