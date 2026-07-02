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

import kotlinx.coroutines.CoroutineDispatcher

// TODO: back this with the LiteRT-LM JVM Kotlin/Java API (docs cite "JVM-based
//  desktop tools via Kotlin/Java APIs"); if that binding is Android-only, fall
//  back to a JNI bridge over the C++ API (plan open item 1).
actual fun createGemmaEngine(ioDispatcher: CoroutineDispatcher): GemmaEngine =
    ScaffoldGemmaEngine("Desktop LiteRT-LM engine not yet wired")
