/*
 * Copyright 2021 Google LLC
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
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}
android {
    namespace = "com.tunjid.heron.feature.template"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":data"))
                implementation(project(":di"))

                implementation(libs.compose.runtime)
                implementation(libs.lifecycle.runtime)
                implementation(libs.lifecycle.viewmodel)

                implementation(libs.kotlinx.coroutines.core)

                api(libs.kotlinx.serialization.protobuf)

                api(libs.tunjid.treenav.core.common)
                api(libs.tunjid.treenav.strings.common)
            }
        }
        named("androidMain") {
            dependencies {
            }
        }
        named("desktopMain") {
            dependencies {
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

