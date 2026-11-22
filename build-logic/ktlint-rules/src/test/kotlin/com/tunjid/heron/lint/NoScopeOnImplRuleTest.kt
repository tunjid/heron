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

class NoScopeOnImplRuleTest {

    private val assertThat = assertThatRule { NoScopeOnImplRule() }

    @Test
    fun allowsUnscopedInjectClasses() {
        assertThat(
            """
            @Inject
            internal class OfflineEventRepository(
                private val eventDao: EventDao,
            ) : EventRepository
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun flagsScopedInjectClasses() {
        assertThat(
            """
            @SingleIn(AppScope::class)
            @Inject
            internal class OfflineEventRepository(
                private val eventDao: EventDao,
            ) : EventRepository
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 1,
            col = 1,
            detail = "Don't scope @Inject classes; put @SingleIn on the provider in di/Bindings.kt",
        )
    }

    @Test
    fun flagsScopedClassesWithInjectConstructors() {
        assertThat(
            """
            @SingleIn(AppScope::class)
            internal class OfflineEventRepository @Inject constructor(
                private val eventDao: EventDao,
            ) : EventRepository
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 1,
            col = 1,
            detail = "Don't scope @Inject classes; put @SingleIn on the provider in di/Bindings.kt",
        )
    }

    @Test
    fun allowsScopedProviders() {
        assertThat(
            """
            @BindingContainer
            object DataBindings {
                @SingleIn(AppScope::class)
                @Provides
                fun provideEventRepository(
                    repository: OfflineEventRepository,
                ): EventRepository = repository
            }
            """.trimIndent(),
        ).hasNoLintViolations()
    }
}
