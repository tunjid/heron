package com.tunjid.heron.feed.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.tunjid.heron.feed.utilities.formatDate
import com.tunjid.heron.feed.utilities.formatTime
import kotlinx.datetime.Instant

@Composable
fun PostDate(
    modifier: Modifier = Modifier,
    time: Instant,
) {
    Text(
        modifier = modifier,
        text = "${time.formatDate()} • ${time.formatTime()}",
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.outline
        ),
    )
}
