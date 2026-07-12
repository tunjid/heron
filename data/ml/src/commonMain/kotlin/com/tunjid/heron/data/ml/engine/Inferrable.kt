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

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Marker for types [infer] can produce from an [InferenceEngine]'s text output.
 */
interface Inferrable

class InferenceParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Runs [prompt] through the engine and decodes the completed output into [T].
 */
suspend inline fun <reified T : Inferrable> InferenceEngine.infer(
    prompt: String,
    params: GenerationParams = GenerationParams(
        temperature = 0.2f,
        maxTokens = 1024,
    ),
): Result<T> {
    val buffer = StringBuilder()
    return try {
        generate(
            prompt = prompt,
            params = params,
        ).collect(buffer::append)
        val output = with(buffer.toString()) {
            val start = indexOf('{')
            val end = lastIndexOf('}')
            if (start != -1 && end > start) substring(start, end + 1) else this
        }

        Result.success(
            InferenceJson.decodeFromString<T>(output),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(
            InferenceParseException(
                message = "Could not infer ${T::class.simpleName} from model output",
                cause = throwable,
            ),
        )
    }
}

@PublishedApi
internal val InferenceJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}
