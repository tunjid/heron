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
        private const val MASK = 0x1FL // Binary 11111, used to get the 5 least significant bits.

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

        /**
         * Encodes a 64-bit Long into a 13-character base32-sortable string.
         *
         * @param value The 64-bit integer TID.
         * @return The encoded 13-character string.
         */
        fun encode(value: Long): String {
            // The spec defines TID for integer zero as a special case.
            if (value == 0L) {
                return "2222222222222"
            }

            // A 64-bit number requires 13 characters in base32 (ceil(64 / 5) = 13).
            val buffer = CharArray(13)
            var current = value

            // We iterate from right to left, filling the buffer.
            for (i in 12 downTo 0) {
                // Get the index for the alphabet by masking the 5 least significant bits.
                val index = (current and MASK).toInt()
                buffer[i] = ALPHABET[index]
                // Right-shift by 5 to process the next 5 bits in the next iteration.
                // `ushr` is an unsigned shift, which is important for handling the sign bit correctly.
                current = current ushr 5
            }
            return buffer.concatToString()
        }
    }
}
