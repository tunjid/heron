package com.tunjid.heron.feed.utilities

import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun Instant.formatDate(): String {
    return SimpleDateFormat
        .getDateInstance(DateFormat.LONG, Locale.getDefault())
        .format(Date.from(toJavaInstant()))
}

@Composable
actual fun Instant.formatTime(): String {
    return SimpleDateFormat
        .getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        .format(Date.from(toJavaInstant()))
}
