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

package com.tunjid.heron.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.live_badge
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileLiveChip(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = LiveStatusColor,
                shape = RoundedCornerShape(LiveChipCorner),
            )
            .padding(
                horizontal = 5.dp,
                vertical = 1.5.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(CommonStrings.live_badge),
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            lineHeight = 10.sp,
        )
    }
}

fun Modifier.profileLiveAvatarBorder() =
    border(
        width = LiveBorderWidth,
        color = LiveStatusColor,
        shape = CircleShape,
    )

private val LiveStatusColor = Color(0xFFE5143A)
private val LiveBorderWidth = 2.5.dp
private val LiveChipCorner = 4.dp
