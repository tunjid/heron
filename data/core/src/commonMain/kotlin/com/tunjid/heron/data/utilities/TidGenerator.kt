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

package com.tunjid.heron.data.utilities

import com.tunjid.heron.data.core.types.RecordKey
import dev.zacsweers.metro.Inject
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A generator for Time-ordered Identifiers (TIDs).
 *
 * TIDs are 64-bit integers that are sortable and encode a timestamp and a random clock identifier.
 * This implementation is thread-safe and guarantees monotonically increasing TIDs.
 *
 * TID Layout (64 bits):
 * - Bit 0: 0 (reserved, always 0)
 * - Bits 1-53 (53 bits): Microseconds since the UNIX epoch.
 * - Bits 54-63 (10 bits): A random clock identifier to prevent collisions.
 */
@OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)
class TidGenerator @Inject constructor() {

    private val mutex = Mutex()

    /**
     * The clock identifier is a random 10-bit number (0-1023) generated once per generator instance
     * to minimize collisions between different processes or machines.
     */
    private val clockId: Long = Random.nextInt(0, 1024).toLong()

    /**
     * Stores the last generated TID value. Using AtomicLong ensures thread-safe reads and writes,
     * which is crucial for guaranteeing monotonicity in a multi-threaded environment.
     */
    private val lastTidValue = AtomicLong(0L)

    /**
     * Generates the next TID as a base32-sortable string.
     *
     * This method is synchronized to ensure that even if called from multiple threads
     * simultaneously, the generated TIDs will be strictly monotonic.
     *
     * @return A 13-character, base32-sortable TID string.
     */
    suspend fun generate(): String =
        mutex.withLock {
            // 1. Get the current time in microseconds since the UNIX epoch.
            // We use millisecond precision from the system and multiply by 1000.
            val timeMicros = Clock.System.now().toEpochMilliseconds() * 1000

            // 2. Get the value of the last TID that was generated.
            val lastVal = lastTidValue.load()

            // 3. Extract the timestamp portion from the last TID.
            // We do this by shifting the 64-bit value right by 10 bits.
            val lastTimeMicros = lastVal ushr 10

            // 4. Determine the next TID value to ensure it's always increasing.
            val nextValue: Long =
                if (timeMicros > lastTimeMicros) {
                    // If the current time is greater than the last recorded time, we can safely
                    // generate a new TID based on the current time and the clock ID.
                    (timeMicros shl 10) or clockId
                } else {
                    // If the current time is not greater (e.g., same microsecond or clock skew),
                    // we must increment the last generated value by 1. This ensures the new TID
                    // is still larger than the previous one, maintaining sort order.
                    lastVal + 1
                }

            // 5. Atomically set the new value as the last generated TID.
            lastTidValue.store(nextValue)

            // 6. Encode the 64-bit integer into its final base32-sortable string format.
            return RecordKey.encode(nextValue)
        }
}
