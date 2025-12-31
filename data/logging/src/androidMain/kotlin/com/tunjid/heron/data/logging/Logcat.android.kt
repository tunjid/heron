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

package com.tunjid.heron.data.logging

import android.content.Context
import logcat.AndroidLogcatLogger
import logcat.LogPriority as SquareLogPriority
import logcat.LogcatLogger
import logcat.asLog as squareAsLog
import logcat.logcat as squareLogcat

class AndroidLogger(
    private val context: Context,
) : Logger {

    override fun install() {
        val shouldLog = when (context.packageName.split(".").lastOrNull()?.lowercase()) {
            DEBUG,
            STAGING,
            -> true
            else -> false
        }
        if (!LogcatLogger.isInstalled && shouldLog) {
            LogcatLogger.install()
            LogcatLogger.loggers += AndroidLogcatLogger(SquareLogPriority.VERBOSE)
        }
    }
}

actual fun Any.logcat(
    priority: LogPriority,
    tag: String?,
    message: () -> String,
) {
    this.squareLogcat(
        priority = when (priority) {
            LogPriority.VERBOSE -> SquareLogPriority.VERBOSE
            LogPriority.DEBUG -> SquareLogPriority.DEBUG
            LogPriority.INFO -> SquareLogPriority.INFO
            LogPriority.WARN -> SquareLogPriority.WARN
            LogPriority.ERROR -> SquareLogPriority.ERROR
            LogPriority.ASSERT -> SquareLogPriority.ASSERT
        },
        tag = tag,
        message = message,
    )
}

actual fun Throwable.loggableText(): String = squareAsLog()

private const val DEBUG = "debug"
private const val STAGING = "staging"
