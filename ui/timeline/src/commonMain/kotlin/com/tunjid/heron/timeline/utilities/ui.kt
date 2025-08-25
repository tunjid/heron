/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tunjid.heron.timeline.utilities

import kotlin.time.Duration

// fun Handle.color(): Color {
//  return Color(0xFF000000 or (hashCode().toLong() and 0x00FFFFFF))
// }

fun Duration.roundComponent() = toComponents { days, hours, minutes, seconds, _ ->
    when {
        days > 0 -> "${days}d"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        seconds < 0 || minutes < 0 || hours < 0 || days < 0 -> "The Future"
        else -> "Now"
    }
}

fun format(value: Long): String = when (value) {
    in 10_000_000_000..Long.MAX_VALUE -> format(value, 1_000_000f, "B", wholeNumber = true)
    in 1_000_000_000..10_000_000_000 -> format(value, 1_000_000_000f, "B")
    in 10_000_000..1_000_000_000 -> format(value, 1_000_000f, "M", wholeNumber = true)
    in 1_000_000..10_000_000 -> format(value, 1_000_000f, "M")
    in 100_000..1_000_000 -> format(value, 1_000f, "K", wholeNumber = true)
    in 10_000..1_000_000 -> format(value, 1_000f, "K")
    else -> format(value, 1f, "", wholeNumber = true)
}

private fun format(
    value: Long,
    div: Float,
    suffix: String,
    wholeNumber: Boolean = false,
): String {
    val msd = (value / div).toInt()
    val lsd = (value * 10 / div).toInt() % 10

    return if (msd >= 1000 && lsd == 0) {
        msd.formatDecimalSeparator()
    } else if (lsd == 0 || wholeNumber) {
        "$msd$suffix"
    } else {
        "$msd.$lsd$suffix"
    }
}

private fun Int.formatDecimalSeparator(): String = toString()
    .reversed()
    .chunked(3)
    .joinToString(separator = ",")
    .reversed()
