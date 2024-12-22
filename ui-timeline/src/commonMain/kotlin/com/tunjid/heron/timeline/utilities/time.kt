package com.tunjid.heron.timeline.utilities

import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant

@Composable
expect fun Instant.formatDate(): String

@Composable
expect fun Instant.formatTime(): String
