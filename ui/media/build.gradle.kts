import com.android.build.gradle.internal.ide.kmp.KotlinAndroidSourceSetMarker.Companion.android

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
    namespace = "com.tunjid.heron.ui.images"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":ui:core"))

                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.components.resources)
                implementation(libs.compose.multiplatform.foundation.layout)
                implementation(libs.compose.multiplatform.foundation.foundation)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui.ui)

                implementation(libs.coil.compose)
                implementation(libs.coil.ktor)

                implementation(libs.filekit.coil)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(libs.tunjid.composables)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.media3.datasource.okhttp)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.exoplayer.dash)
                implementation(libs.androidx.media3.exoplayer.hls)

                implementation(libs.andrew.bailey.difference)
                implementation(libs.coil.gif.android)
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
