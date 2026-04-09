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
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.configureFeatureModule(
    extension: KotlinMultiplatformExtension,
) = extension.apply {
    sourceSets.apply {
        named("commonMain") {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:core"))
                implementation(project(":data:logging"))
                implementation(project(":scaffold"))
                implementation(project(":feature:template"))
                implementation(project(":ui:core"))
                implementation(project(":ui:icons"))
                implementation(project(":ui:media"))
                implementation(project(":ui:tiling"))
                implementation(project(":ui:timeline"))

                api(libs.lifecycle.multiplatform.viewmodel)
                api(libs.lifecycle.multiplatform.viewmodel.compose)

                implementation(libs.navigation.event.compose)
            }
        }
    }
}
