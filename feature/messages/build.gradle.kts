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
    id("kotlin-library-convention")
    id("feature-module-convention")
    id("ksp-convention")
}
kotlin {
    androidLibrary {
        namespace = "com.tunjid.heron.feature.messages"
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":data:core"))
                implementation(project(":scaffold"))
                implementation(project(":feature:template"))

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.tunjid.tiler.tiler)
                implementation(libs.tunjid.tiler.compose)

                // Provides the androidx.compose.ui.tooling.preview.Preview annotation
                // used by MessagesScreenPreview. The compose-preview renderer plugin
                // itself is auto-injected by the compose-preview CLI.
                implementation(libs.compose.multiplatform.ui.tooling.preview)
            }
        }
        named("desktopMain") {
            dependencies {
                // Host skiko native for the compose-preview Desktop renderer (the CLI injects the plugin, not skiko).
                runtimeOnly(compose.desktop.currentOs)
            }
        }
    }
}
