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
    id("kotlin-library-convention")
    id("ui-module-convention")
}
kotlin {
    androidLibrary {
        namespace = "com.tunjid.heron.ui.profile"
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":ui:core"))
                implementation(project(":ui:media"))

                implementation(libs.coil.compose)
                implementation(libs.coil.ktor)

                implementation(libs.filekit.coil)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.kotlinx.datetime)
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
