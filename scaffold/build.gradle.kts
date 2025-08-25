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
    id("ksp-convention")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.compose")
}

android {
    namespace = "com.tunjid.heron.domain.navigation"
}

compose.resources {
    publicResClass = true
}

kotlin {
    sourceSets {
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation("org.jetbrains.androidx.core:core-bundle:1.1.0-alpha03")
            }
        }
        commonMain {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:core"))
                implementation(project(":ui:core"))
                implementation(project(":ui:media"))

                implementation(libs.androidx.navigation.event)

                implementation(libs.compose.animation)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.foundation.foundation)
                implementation(libs.compose.foundation.layout)
                implementation(libs.compose.runtime)
                implementation(libs.compose.material)
                implementation(libs.compose.material.icons)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.okio)

                implementation(libs.savedstate.compose)
                implementation(libs.savedstate.savedstate)

                implementation(libs.tunjid.composables)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)

                implementation(libs.tunjid.treenav.compose)
                implementation(libs.tunjid.treenav.compose.threepane)
                implementation(libs.tunjid.treenav.core)
                implementation(libs.tunjid.treenav.strings)
            }
        }
    }
}
