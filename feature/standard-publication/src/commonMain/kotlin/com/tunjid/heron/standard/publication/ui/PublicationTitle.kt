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

package com.tunjid.heron.standard.publication.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.models.StandardPublication
import com.tunjid.heron.data.core.models.StandardSubscription
import com.tunjid.heron.images.AsyncImage
import com.tunjid.heron.images.ImageArgs
import com.tunjid.heron.timeline.utilities.avatarSharedElementKey
import com.tunjid.heron.timeline.utilities.collectionShape
import com.tunjid.heron.ui.AppBarButton
import com.tunjid.heron.ui.PaneTransitionScope
import com.tunjid.heron.ui.UiTokens
import com.tunjid.heron.ui.rememberLatchedState
import com.tunjid.heron.ui.text.CommonStrings
import heron.feature.standard_publication.generated.resources.Res
import heron.feature.standard_publication.generated.resources.publication_publisher
import heron.ui.core.generated.resources.subscription_subscribed
import heron.ui.core.generated.resources.subscription_unsubscribed
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PublicationTitle(
    modifier: Modifier = Modifier,
    paneTransitionScope: PaneTransitionScope,
    sharedElementPrefix: String?,
    publication: StandardPublication?,
) = with(paneTransitionScope) {
    if (publication != null) Row(
        modifier = modifier
            .padding(
                horizontal = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaneStickySharedElement(
            modifier = Modifier
                .size(44.dp),
            sharedContentState = rememberSharedContentState(
                key = publication.avatarSharedElementKey(
                    prefix = sharedElementPrefix,
                    creator = StandardPublication::publisher,
                ),
            ),
            zIndexInOverlay = UiTokens.higherThanAppBarSharedElementZIndex(),
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillParentAxisIfFixedOrWrap(),
                args = remember(publication.icon) {
                    ImageArgs(
                        url = publication.icon?.uri,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        shape = publication.collectionShape(),
                    )
                },
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f),
        ) {
            Column {
                Text(
                    modifier = Modifier,
                    text = publication.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    modifier = Modifier,
                    text = stringResource(
                        Res.string.publication_publisher,
                        publication.publisher.handle.id,
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun SubscribeButton(
    modifier: Modifier = Modifier,
    publication: StandardPublication,
    onSubscriptionToggled: (StandardPublication, StandardSubscription?) -> Unit,
) {
    val subscription = publication.subscription
    val latchedSubscribedState = rememberLatchedState(subscription != null)
    val subscribed by latchedSubscribedState

    AnimatedContent(
        modifier = modifier,
        targetState = subscribed,
        transitionSpec = {
            SubscriptionContentTransform
        },
    ) { isSubscribed ->
        AppBarButton(
            icon =
            if (isSubscribed) Icons.Rounded.NotificationsActive
            else Icons.Rounded.NotificationsOff,
            iconDescription = stringResource(
                if (isSubscribed) CommonStrings.subscription_subscribed
                else CommonStrings.subscription_unsubscribed,
            ),
            onClick = {
                if (latchedSubscribedState.isCurrent) {
                    onSubscriptionToggled(
                        publication,
                        subscription,
                    )
                    latchedSubscribedState.latch(!subscribed)
                }
            },
        )
    }
}

private val SubscriptionContentTransform = fadeIn() togetherWith fadeOut()
