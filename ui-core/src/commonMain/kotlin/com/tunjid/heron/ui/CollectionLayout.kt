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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun CollectionLayout(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    description: String?,
    blurb: String?,
    avatar: @Composable () -> Unit,
    action: @Composable (() -> Unit)? = null,
    onClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable { onClicked() }
            .padding(
                vertical = 4.dp,
                horizontal = 16.dp
            ),
    ) {
        AttributionLayout(
            modifier = Modifier
                .fillMaxWidth(),
            avatar = avatar,
            label = {
                Text(
                    text = title,
                    style = LocalTextStyle.current.copy(fontWeight = Bold),
                )
                Text(
                    text = subtitle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            },
            action = action,
        )
        Spacer(
            modifier = Modifier
                .height(8.dp)
        )
        Text(
            text = description ?: "",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(
            modifier = Modifier
                .height(8.dp)
        )
        Text(
            text = blurb ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(
            modifier = Modifier
                .height(4.dp)
        )
    }
}