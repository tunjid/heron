package com.tunjid.heron.feed.utilities

import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle

@Composable
actual fun Instant.formatDate(): String {
    return NSDateFormatter()
        .apply { dateStyle = NSDateFormatterShortStyle }
        .stringFromDate(toNSDate())
}

@Composable
actual fun Instant.formatTime(): String {
    return NSDateFormatter()
        .apply { timeStyle = NSDateFormatterShortStyle }
        .stringFromDate(toNSDate())
}
