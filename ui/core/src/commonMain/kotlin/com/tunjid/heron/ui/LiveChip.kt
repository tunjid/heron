package com.tunjid.heron.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tunjid.heron.ui.UiTokens.LiveChipCorner
import com.tunjid.heron.ui.UiTokens.LiveStatusColor
import com.tunjid.heron.ui.text.CommonStrings
import heron.ui.core.generated.resources.live_badge
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiveChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = LiveStatusColor, shape = RoundedCornerShape(LiveChipCorner))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
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
