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

package com.tunjid.heron.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tunjid.heron.data.core.types.RecordUri
import com.tunjid.heron.ui.text.rememberFormattedTextPost
import com.tunjid.treenav.compose.MovableElementSharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecordLayout(
    modifier: Modifier = Modifier,
    movableElementSharedTransitionScope: MovableElementSharedTransitionScope,
    title: String,
    subtitle: String,
    description: String?,
    blurb: String?,
    sharedElementPrefix: String?,
    sharedElementType: RecordUri,
    avatar: @Composable () -> Unit,
    action: @Composable (() -> Unit)? = null,
) = with(movableElementSharedTransitionScope) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AttributionLayout(
            modifier = Modifier
                .fillMaxWidth(),
            avatar = avatar,
            label = {
                PaneStickySharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = titleSharedElementKey(
                            prefix = sharedElementPrefix,
                            type = sharedElementType,
                        ),
                    ),
                ) {
                    Text(
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = LocalTextStyle.current.copy(fontWeight = Bold),
                    )
                }
                PaneStickySharedElement(
                    sharedContentState = rememberSharedContentState(
                        key = subtitleSharedElementKey(
                            prefix = sharedElementPrefix,
                            type = sharedElementType,
                        ),
                    ),
                ) {
                    Text(
                        text = subtitle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            action = action,
        )
        description.takeUnless(String?::isNullOrEmpty)?.let {
            Text(
                text = rememberFormattedTextPost(it),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        blurb.takeUnless(String?::isNullOrEmpty)?.let {
            Text(
                text = rememberFormattedTextPost(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun titleSharedElementKey(
    prefix: String?,
    type: RecordUri,
): String = "$prefix-$type-title"

fun subtitleSharedElementKey(
    prefix: String?,
    type: RecordUri,
): String = "$prefix-$type-subtitle"
