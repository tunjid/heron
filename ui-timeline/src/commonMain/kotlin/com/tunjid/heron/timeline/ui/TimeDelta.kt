package com.tunjid.heron.timeline.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Duration

@Composable
fun TimeDelta(
    delta: Duration,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = delta.toComponents { days, hours, minutes, seconds, _ ->
            when {
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                seconds > 0 -> "${seconds}s"
                seconds < 0 || minutes < 0 || hours < 0 || days < 0 -> "The Future"
                else -> "Now"
            }
        },
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}
