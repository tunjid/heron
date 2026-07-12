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

package com.tunjid.heron.data.ml.engine

import com.tunjid.heron.data.ml.model.LoadedModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable

/** A private, [Serializable] [Inferrable] — proves an out-of-module type works as `T`. */
@Serializable
private data class Sample(
    val topic: String,
    val limit: Int = 0,
) : Inferrable

/** An [InferenceEngine] whose [generate] replays [flow]; the rest is inert. */
private class CannedEngine(
    private val flow: Flow<String>,
) : InferenceEngine {
    override val state: Flow<EngineState> = MutableStateFlow(EngineState.Uninitialized)
    override suspend fun load(model: LoadedModel) = Unit
    override fun generate(
        prompt: String,
        params: GenerationParams,
    ): Flow<String> = flow

    override suspend fun reset() = Unit
}

private fun engineEmitting(
    vararg tokens: String,
): InferenceEngine = CannedEngine(tokens.asFlow())

class InferrableTest {

    @Test
    fun decodesJsonStreamedAcrossChunks() = runTest {
        val engine = engineEmitting(
            """{"topic":""",
            """ "atproto",""",
            """ "limit": 5}""",
        )

        val result = engine.infer<Sample>(prompt = "irrelevant")

        assertEquals(
            expected = Sample(topic = "atproto", limit = 5),
            actual = result.getOrNull(),
        )
    }

    @Test
    fun stripsMarkdownFencesAndPreamble() = runTest {
        val engine = engineEmitting(
            "Sure! Here is the filter:\n```json\n{\"topic\": \"cats\"}\n```\nHope that helps.",
        )

        val result = engine.infer<Sample>(prompt = "irrelevant")

        assertEquals(
            expected = Sample(topic = "cats"),
            actual = result.getOrNull(),
        )
    }

    @Test
    fun ignoresUnknownKeys() = runTest {
        val engine = engineEmitting("""{"topic": "dogs", "mood": "happy"}""")

        val result = engine.infer<Sample>(prompt = "irrelevant")

        assertEquals(
            expected = Sample(topic = "dogs"),
            actual = result.getOrNull(),
        )
    }

    @Test
    fun failsWhenNoJsonPresent() = runTest {
        val engine = engineEmitting("I can't help with that request.")

        val result = engine.infer<Sample>(prompt = "irrelevant")

        assertTrue(result.isFailure)
        assertIs<InferenceParseException>(result.exceptionOrNull())
    }

    @Test
    fun wrapsGenerationErrorsAsFailure() = runTest {
        val engine = CannedEngine(flow { throw IllegalStateException("engine boom") })

        val result = engine.infer<Sample>(prompt = "irrelevant")

        assertTrue(result.isFailure)
        assertIs<InferenceParseException>(result.exceptionOrNull())
    }
}
