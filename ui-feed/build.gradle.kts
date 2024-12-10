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
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.tunjid.heron.ui.feed"
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":ui-images"))

                implementation(libs.compose.runtime)
                implementation(libs.compose.ui.ui)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
