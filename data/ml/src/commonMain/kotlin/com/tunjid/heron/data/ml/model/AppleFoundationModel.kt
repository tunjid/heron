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

package com.tunjid.heron.data.ml.model

/**
 * The Apple Foundation Models on-device system model. The OS owns the weights, so there is nothing
 * to download; availability is resolved at runtime by the iOS inference manager. A [Platform] model
 * has no execution home on other platforms — it's only ever surfaced where the capability is
 * [com.tunjid.heron.data.ml.engine.InferenceSource.Platform].
 */
object AppleFoundationModel : InferenceModel.Platform {
    override val name: String = "Apple Intelligence"

    // The on-device system model's context window (prompt + response), per Apple's guidance.
    override val maxTokens: Int = 4096

    override val abilities: List<InferenceModel.Ability> = listOf(
        InferenceModel.Ability.Translation,
        InferenceModel.Ability.Summary,
    )
}
