package com.tunjid.heron.feed.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tunjid.heron.feed.utilities.formatDate
import com.tunjid.heron.feed.utilities.formatTime
import kotlinx.datetime.Instant

@Composable
fun PostDate(time: Instant) {
  Text(
    text = "${time.formatDate()} • ${time.formatTime()}",
  )
}
