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
    sharedElementType: Any,
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
                Text(
                    modifier = Modifier
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = titleSharedElementKey(
                                    prefix = sharedElementPrefix,
                                    type = sharedElementType,
                                ),
                            ),
                        ),
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = LocalTextStyle.current.copy(fontWeight = Bold),
                )
                Text(
                    modifier = Modifier
                        .paneStickySharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = subtitleSharedElementKey(
                                    prefix = sharedElementPrefix,
                                    type = sharedElementType,
                                ),
                            ),
                        ),
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            },
            action = action,
        )
        description.takeUnless(String?::isNullOrEmpty)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        blurb.takeUnless(String?::isNullOrEmpty)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

fun titleSharedElementKey(
    prefix: String?,
    type: Any,
): String = "$prefix-$type-title"

fun subtitleSharedElementKey(
    prefix: String?,
    type: Any,
): String = "$prefix-$type-subtitle"
