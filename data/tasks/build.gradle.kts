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
        namespace = "com.tunjid.heron.data.tasks"
    }
}

kotlin {
    applyDefaultHierarchyTemplate()
    sourceSets {
        // Shared by the JVM-family targets (Android + desktop), which download over Ktor.
        val httpMain by creating {
            dependsOn(commonMain.get())
        }
        commonMain {
            dependencies {
                implementation(project(":data:files"))
                implementation(project(":data:logging"))

                // FileManager (:data:files) exposes okio types in its API and is referenced here, so
                // okio is needed on every target (iOS isn't in httpMain). :data:files keeps okio as
                // an implementation detail, so consumers depend on it directly.
                implementation(libs.okio)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.core)
                implementation(libs.ktor.client.logging)
            }
        }
        androidMain {
            dependsOn(httpMain)
            dependencies {
                implementation(libs.androidx.work.runtime)
                implementation(libs.androidx.core.ktx)
            }
        }
        desktopMain {
            dependsOn(httpMain)
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
    }
}
