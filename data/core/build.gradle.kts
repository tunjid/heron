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
    id("ksp-convention")
    kotlin("plugin.serialization")
}
android {
    namespace = "com.tunjid.heron.data"
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:database"))
                implementation(project(":data:lexicons"))

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.ktor.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.resources)
                implementation(libs.ktor.serialization.kotlinx.json)

                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.androidx.datastore.core.okio)

                implementation(libs.connectivity.core)

                implementation(libs.okio)
                implementation(libs.ozone.atproto.runtime)

                implementation(libs.tunjid.tiler.tiler)
                implementation(libs.tunjid.mutator.core.common)
                implementation(libs.tunjid.mutator.coroutines.common)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core.ktx)

                implementation(libs.ktor.client.android)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
