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
    id("ee.schimke.composeai.preview") version "0.13.4"
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

                implementation(libs.compose.multiplatform.ui.tooling.preview)
            }
        }
        named("desktopMain") {
            dependencies {
                // Host skiko native binaries for the compose-preview Desktop (Skia)
                // renderer. A feature module's desktop classpath has no
                // skiko-awt-runtime-<host>, so without this the renderer falls back
                // to a mismatched native lib (UnsatisfiedLinkError: _nSetFontEdging).
                // Resolved per host so the wrong arch never leaks into the desktop app.
                val osName = System.getProperty("os.name").lowercase()
                val osArch = System.getProperty("os.arch").lowercase()
                val skikoTarget = when {
                    osName.contains("linux") && osArch.contains("aarch64") -> "linux-arm64"
                    osName.contains("linux") -> "linux-x64"
                    osName.contains("mac") && osArch.contains("aarch64") -> "macos-arm64"
                    osName.contains("mac") -> "macos-x64"
                    osName.contains("windows") -> "windows-x64"
                    else -> null
                }
                if (skikoTarget != null) {
                    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-$skikoTarget:0.144.6")
                }
            }
        }
    }
}
