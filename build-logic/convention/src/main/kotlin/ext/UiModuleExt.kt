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

import ext.libs
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun org.gradle.api.Project.configureUiModule(
    extension: KotlinMultiplatformExtension,
) = extension.apply {
    sourceSets.apply {
        named("commonMain") {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:core"))
                implementation(project(":scaffold"))
                implementation(project(":feature:template"))
                implementation(project(":ui:core"))
                implementation(project(":ui:media"))
                implementation(project(":ui:tiling"))
                implementation(project(":ui:timeline"))

                api(libs.findLibrary("lifecycle-runtime").get())
                api(libs.findLibrary("lifecycle-runtime-compose").get())
                api(libs.findLibrary("lifecycle-viewmodel").get())
                api(libs.findLibrary("lifecycle-viewmodel-compose").get())

                api(libs.findLibrary("compose-components-resources").get())
                api(libs.findLibrary("compose-runtime").get())
                api(libs.findLibrary("compose-animation").get())
                api(libs.findLibrary("compose-material-icons-extended").get())
                api(libs.findLibrary("compose-material3").get())
                api(libs.findLibrary("compose-foundation-layout").get())

                api(libs.findLibrary("androidx-graphics-shapes").get())

                api(libs.findLibrary("kotlinx-coroutines-core").get())

                api(libs.findLibrary("savedstate-savedstate").get())
                api(libs.findLibrary("savedstate-compose").get())

                api(libs.findLibrary("tunjid-mutator-core-common").get())
                api(libs.findLibrary("tunjid-mutator-coroutines-common").get())

                api(libs.findLibrary("tunjid-composables").get())

                api(libs.findLibrary("tunjid-treenav-compose").get())
                api(libs.findLibrary("tunjid-treenav-compose-threepane").get())
                api(libs.findLibrary("tunjid-treenav-core").get())
                api(libs.findLibrary("tunjid-treenav-strings").get())
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
//        named("commonTest") {
//            dependencies {
//                implementation(kotlin("test"))
//            }
//        }
    }
}
