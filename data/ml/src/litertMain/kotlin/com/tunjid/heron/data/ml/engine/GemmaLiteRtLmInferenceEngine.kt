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
import com.tunjid.heron.data.ml.model.LoadedModel
import com.tunjid.heron.data.ml.model.asGemma
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [InferenceEngine] backed by the LiteRT-LM `Engine`/`Conversation` API. Shared by the
 * Android (`litertlm-android`) and desktop JVM (`litertlm-jvm`) targets, whose
 * `com.google.ai.edge.litertlm` API is identical.
 */
internal class GemmaLiteRtLmInferenceEngine(
    private val ioDispatcher: CoroutineDispatcher,
) : InferenceEngine {

    // Serializes access to the mutable [engine] across load/generate/close.
    private val mutex = Mutex()

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private var engine: Engine? = null

    override suspend fun load(
        model: LoadedModel,
    ) = mutex.withLock {
        _state.value = EngineState.Loading
        try {
            withContext(ioDispatcher) {
                engine?.close()
                engine = null
                val newEngine = Engine(
                    EngineConfig(
                        modelPath = model.path.toString(),
                        backend = Backend.CPU(),
                        maxNumTokens = model.model.asGemma().defaultConfig.maxTokens,
                    ),
                )
                try {
                    newEngine.initialize()
                    engine = newEngine
                } catch (throwable: Throwable) {
                    newEngine.close()
                    throw throwable
                }
            }
            _state.value = EngineState.Ready(model)
        } catch (throwable: Throwable) {
            _state.value = EngineState.Error(
                throwable.message ?: "Failed to load model",
                throwable,
            )
            throw throwable
        }
    }

    override fun generate(
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

    override suspend fun close() = mutex.withLock {
        withContext(ioDispatcher) {
            engine?.close()
            engine = null
        }
        _state.value = EngineState.Uninitialized
    }

    private fun textOf(message: Message): String =
        message.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
}

/** Builds the LiteRT-LM engine for Android and desktop; call from the app entry point. */
fun createInferenceEngine(ioDispatcher: CoroutineDispatcher): InferenceEngine =
    GemmaLiteRtLmInferenceEngine(ioDispatcher)
