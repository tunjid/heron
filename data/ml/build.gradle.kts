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
    id("ksp-convention")
    kotlin("plugin.serialization")
}

kotlin {
    androidLibrary {
        namespace = "com.tunjid.heron.data.ml"
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.core)
                implementation(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        // androidMain / iosMain / desktopMain hold the platform `createGemmaEngine`
        // actuals. They intentionally carry no LiteRT-LM SDK dependency yet — the
        // real bindings land once the artifact coordinates are confirmed (see plan
        // open items 1-2). Until then the actuals are compile-green scaffolds.
    }
}
