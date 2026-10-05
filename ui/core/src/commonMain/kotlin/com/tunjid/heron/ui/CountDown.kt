package com.tunjid.heron.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class CountDown(
    private val duration: Duration = 800.milliseconds,
) {

    val lapsed
        get(): Boolean {
            val changedAt = resetAt ?: return true
            val elapsed = createdTime.elapsedNow()
            val diff = elapsed - changedAt
            return diff.isPositive() && diff > duration
        }

    private val createdTime = TimeSource.Monotonic.markNow()
    private var resetAt by mutableStateOf<Duration?>(null)

    fun reset() {
        resetAt = createdTime.elapsedNow()
    }
}
