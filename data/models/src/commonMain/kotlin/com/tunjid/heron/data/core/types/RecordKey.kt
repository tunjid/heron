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

package com.tunjid.heron.data.core.types

import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class RecordKey(
    val value: String,
) {
    companion object {
        private const val ALPHABET = "234567abcdefghijklmnopqrstuvwxyz"

        /**
         * Generates a new [RecordKey] with a valid AT Protocol TID.
         *
         * This method should not be used in concurrent environments as it
         * does not guarantee that the tid monotonically increases.
         */
        @OptIn(ExperimentalTime::class)
        fun generate(): RecordKey {
            val timeMicros = Clock.System.now().toEpochMilliseconds() * 1000
            val clockId = Random.nextInt(0, 1024)
            val tidInt = (timeMicros shl 10) or clockId.toLong()
            return RecordKey(encode(tidInt))
        }

        private fun encode(value: Long): String {
            if (value == 0L) return "2222222222222"
            val buffer = CharArray(13)
            var current = value
            for (i in 12 downTo 0) {
                val index = (current and 0x1FL).toInt()
                buffer[i] = ALPHABET[index]
                current = current ushr 5
            }
            return buffer.concatToString()
        }
    }
}
