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

import ext.libs
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun org.gradle.api.Project.configureUiModule(
    extension: KotlinMultiplatformExtension,
) = extension.apply {
    sourceSets.apply {
        named("commonMain") {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:core"))
                implementation(project(":data:logging"))
                implementation(project(":scaffold"))
                implementation(project(":feature:template"))
                implementation(project(":ui:core"))
                implementation(project(":ui:media"))
                implementation(project(":ui:tiling"))
                implementation(project(":ui:timeline"))

                api(libs.lifecycle.multiplatform.runtime)
                api(libs.lifecycle.multiplatform.runtime.compose)
                api(libs.lifecycle.multiplatform.viewmodel)
                api(libs.lifecycle.multiplatform.viewmodel.compose)

                api(libs.compose.multiplatform.components.resources)
                api(libs.compose.multiplatform.runtime)
                api(libs.compose.multiplatform.animation)
                api(libs.compose.multiplatform.material.icons.extended)
                api(libs.compose.multiplatform.material3)
                api(libs.compose.multiplatform.foundation.layout)

                api(libs.androidx.graphics.shapes)

                api(libs.kotlinx.coroutines.core)

                api(libs.savedstate.multiplatform.savedstate)
                api(libs.savedstate.multiplatform.compose)

                api(libs.tunjid.mutator.core.common)
                api(libs.tunjid.mutator.coroutines.common)
                api(libs.tunjid.mutator.compose )

                api(libs.tunjid.composables)

                api(libs.tunjid.treenav.compose)
                api(libs.tunjid.treenav.compose.threepane)
                api(libs.tunjid.treenav.core)
                api(libs.tunjid.treenav.strings)
            }
        }
    }
}
