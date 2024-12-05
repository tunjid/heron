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
    kotlin("plugin.serialization")
    id("androidx.room")
}
android {
    namespace = "com.tunjid.heron.data"
    buildFeatures {
        compose = false
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
                api(project(":di"))
                api(project(":data-core"))

                implementation(libs.kotlinx.datetime)

                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite)
                implementation(libs.androidx.sqlite.bundled)

                implementation(libs.okio)
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
dependencies {
    implementation(project(":di"))
}
