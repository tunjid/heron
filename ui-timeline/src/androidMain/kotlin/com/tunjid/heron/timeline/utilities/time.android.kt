package com.tunjid.heron.timeline.utilities

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date

@Composable
actual fun Instant.formatDate(): String {
    return DateFormat
        .getDateFormat(LocalContext.current)
        .format(Date.from(toJavaInstant()))
}

@Composable
actual fun Instant.formatTime(): String {
    return DateFormat
        .getTimeFormat(LocalContext.current)
        .format(Date.from(toJavaInstant()))
}

