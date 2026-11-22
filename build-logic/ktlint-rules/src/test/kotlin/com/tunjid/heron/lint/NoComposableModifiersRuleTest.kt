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

class NoComposableModifiersRuleTest {

    private val assertThat = assertThatRule { NoComposableModifiersRule() }

    @Test
    fun allowsNodeModifiers() {
        assertThat(
            """
            fun Modifier.paneClip(
                state: PaneClipState,
            ): Modifier = this then PaneClipElement(
                state = state,
            )
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun allowsComposablesTakingModifiers() {
        assertThat(
            """
            @Composable
            fun Avatar(
                modifier: Modifier = Modifier,
            ) = Unit
            """.trimIndent(),
        ).hasNoLintViolations()
    }

    @Test
    fun flagsComposableModifiers() {
        assertThat(
            """
            @Composable
            fun Modifier.sharedBounds(
                state: PaneState,
            ): Modifier = this
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 2,
            col = 14,
            detail = "Modifiers must not be @Composable; build them from a Modifier.Node element",
        )
    }

    @Test
    fun flagsComposedModifiers() {
        assertThat(
            """
            fun Modifier.sharedBounds(
                state: PaneState,
            ): Modifier = composed {
                this
            }
            """.trimIndent(),
        ).hasLintViolationWithoutAutoCorrect(
            line = 3,
            col = 15,
            detail = "Don't use Modifier.composed; build the modifier from a Modifier.Node element",
        )
    }
}
