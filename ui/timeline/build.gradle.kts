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

plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("org.jetbrains.compose")
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.tunjid.heron.ui.timeline"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:core"))

                implementation(project(":ui:core"))
                implementation(project(":ui:media"))
                implementation(project(":ui:tiling"))

                implementation(libs.compose.multiplatform.components.resources)
                implementation(libs.compose.multiplatform.foundation.foundation)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui.ui)

                implementation(libs.androidx.graphics.shapes)
                implementation(libs.androidx.collection)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.tunjid.composables)
                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
                implementation(libs.tunjid.tiler.tiler)
                implementation(libs.tunjid.treenav.compose)
                implementation(libs.tunjid.treenav.compose.threepane)
            }
        }
    }
}
