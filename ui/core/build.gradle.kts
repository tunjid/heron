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

compose.resources {
    publicResClass = true
}

android {
    namespace = "com.tunjid.heron.ui.core"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":data:models"))

                implementation(libs.compose.multiplatform.components.resources)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.foundation.layout)
                implementation(libs.compose.multiplatform.foundation.foundation)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui.ui)

                implementation(libs.androidx.graphics.shapes)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(libs.tunjid.composables)

                implementation(libs.tunjid.treenav.compose)
                implementation(libs.tunjid.treenav.compose.threepane)
                implementation(libs.tunjid.treenav.core)
                implementation(libs.tunjid.treenav.strings)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.android)
                api(libs.compose.android.animation)
                api(libs.compose.android.foundation.foundation)
                api(libs.compose.android.foundation.layout)
                api(libs.compose.android.ui.ui)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
//        iosMain {
//            dependencies {
//                implementation(libs.ktor.client.darwin)
//            }
//        }
    }
}
