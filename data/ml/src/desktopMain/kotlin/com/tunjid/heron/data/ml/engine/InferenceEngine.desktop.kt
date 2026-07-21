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

// LiteRT-LM ships no GPU delegate for the desktop JVM; always run on CPU.
internal actual fun backendFor(): InferenceBackend = InferenceBackend.Cpu

// litertlm-jvm's JNI reads the prompt as modified UTF-8, so any emoji / supplementary character
// becomes ill-formed standard UTF-8 and crashes its native JSON parser. Strip those before sending.
internal actual fun sanitizeEnginePrompt(prompt: String): String =
    stripModifiedUtf8Hazards(prompt)
