/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ksp-convention")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.tunjid.heron.domain.navigation"
    buildFeatures {
        compose = true
    }
}

kotlin {
    sourceSets {
        named("androidMain") {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
        named("commonMain") {
            dependencies {
                implementation(project(":data"))

                implementation(libs.compose.runtime)
                implementation(libs.compose.animation)
                implementation(libs.compose.material)
                implementation(libs.compose.material.icons)
                api(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)
                implementation(libs.compose.foundation.foundation)
                implementation(libs.compose.foundation.layout)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.okio)

                implementation(libs.tunjid.composables)

                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)

                implementation(libs.tunjid.treenav.compose.common)
                implementation(libs.tunjid.treenav.core.common)
                implementation(libs.tunjid.treenav.strings.common)
            }
        }
    }
}

