package com.tunjid.heron.timeline.ui.post

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tunjid.heron.timeline.utilities.formatDate
import com.tunjid.heron.timeline.utilities.formatTime
import kotlinx.datetime.Instant

@Composable
internal fun PostDate(
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
