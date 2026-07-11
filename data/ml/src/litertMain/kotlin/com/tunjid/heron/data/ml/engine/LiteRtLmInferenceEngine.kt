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

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.tunjid.heron.data.logging.LogPriority
import com.tunjid.heron.data.logging.logcat
import com.tunjid.heron.data.logging.loggableText
import com.tunjid.heron.data.ml.model.LoadedModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class LiteRtLmInferenceEngine(
    private val ioDispatcher: CoroutineDispatcher,
) : BaseInferenceEngine() {

    private var engine: Engine? = null

    override suspend fun onLoad(
        model: LoadedModel,
    ) = withContext(ioDispatcher) {
        engine?.close()
        engine = null
        val newEngine = Engine(
            EngineConfig(
                modelPath = model.file.relativePath,
                backend = litertBackend(),
                maxNumTokens = model.model.maxTokens,
            ),
        )
        try {
            newEngine.initialize()
            engine = newEngine
        } catch (throwable: Throwable) {
            logcat(LogPriority.ERROR) {
                "Model initialization failed: ${throwable.loggableText()}"
            }
            newEngine.close()
            throw throwable
        }
    }

    override suspend fun onReset() = withContext(ioDispatcher) {
        engine?.close()
        engine = null
    }

    override fun onGenerate(
        prompt: String,
        params: GenerationParams,
    ): Flow<String> = callbackFlow {
        val activeEngine = mutex.withLock {
            checkNotNull(engine) { "generate() called before load()" }
        }
        val conversation = activeEngine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = params.topK,
                    topP = params.topP.toDouble(),
                    temperature = params.temperature.toDouble(),
                ),
            ),
        )
        conversation.sendMessageAsync(
            text = prompt,
            callback = object : MessageCallback {
                override fun onMessage(message: Message) {
                    trySend(textOf(message))
                }

                override fun onDone() {
                    close()
                }

                override fun onError(throwable: Throwable) {
                    close(throwable)
                }
            },
        )

        awaitClose {
            conversation.cancelProcess()
            conversation.close()
        }
    }.flowOn(ioDispatcher)

    private fun textOf(message: Message): String =
        message.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
}

/** Maps the device/platform [backendFor] decision onto the LiteRT-LM [Backend] to run on. */
private fun litertBackend(): Backend =
    when (backendFor()) {
        InferenceBackend.Cpu -> Backend.CPU()
        InferenceBackend.Gpu -> Backend.GPU()
    }

/** Builds the LiteRT-LM engine for Android and desktop; call from the app entry point. */
fun createInferenceEngine(
    ioDispatcher: CoroutineDispatcher,
): InferenceEngine =
    LiteRtLmInferenceEngine(ioDispatcher)
