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

import androidx.compose.runtime.Composable
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Composable
actual fun Instant.formatDate(): String {
    return SimpleDateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
        .format(Date.from(toJavaInstant()))
}

@Composable
actual fun Instant.formatTime(): String {
    return SimpleDateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        .format(Date.from(toJavaInstant()))
}
