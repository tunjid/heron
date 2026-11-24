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

package com.tunjid.heron.lint

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class DaoReactiveReadsRuleTest {

    private val assertThat = assertThatRule { DaoReactiveReadsRule() }

    @Test
    fun allowsFlowReads() {
        assertThat(
            """
            @Dao
            interface EventDao {
                @Query("SELECT * FROM events")
                fun events(): Flow<List<EventEntity>>
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun allowsSuspendingWrites() {
        assertThat(
            """
            @Dao
            interface EventDao {
                @Query("UPDATE events SET count = count + 1")
                suspend fun increment()
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun flagsSuspendingReads() {
        assertThat(
            """
            @Dao
            interface EventDao {
                @Query("SELECT * FROM events")
                suspend fun events(): List<EventEntity>
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 5,
            detail = "DAO reads must be reactive: return a Flow instead of suspending",
        )
    }

    @Test
    fun flagsNonFlowReads() {
        assertThat(
            """
            @Dao
            interface EventDao {
                @Query("WITH recent AS (SELECT * FROM events) SELECT * FROM recent")
                fun events(): List<EventEntity>
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 5,
            detail = "DAO reads must return a Flow",
        )
    }

    @Test
    fun ignoresNonDaoClasses() {
        assertThat(
            """
            interface EventRepository {
                @Query("SELECT * FROM events")
                suspend fun events(): List<EventEntity>
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }
}
