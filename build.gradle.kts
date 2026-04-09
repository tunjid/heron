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

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.axionRelease) apply false
    alias(libs.plugins.buildConfig) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidxRoom) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jetbrainsKotlinJvm) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.ozoneLexiconGenerator) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.aboutLibraries) apply false
    alias(libs.plugins.burst) apply false
}

// 1. Register the lifecycle task (Lazy)
val testDataLayer by tasks.registering {
    group = "verification"
    description = "Runs tests for all data modules"
}

allprojects {
    plugins.apply(rootProject.libs.plugins.spotless.get().pluginId)
    extensions.configure<SpotlessExtension> {
        kotlin {
            target(
                "src/**/*.kt",
                "build-logic/**/*.kt",
                "**/*.kts",
            )
            targetExclude("**/build/**")
            ktlint(rootProject.libs.ktlint.get().version)
        }
    }
}

subprojects {
    // Only apply if the project path matches your criteria
    if (path.startsWith(":data:")) {
        // We use .configure {} to safely add dependencies to the root task
        testDataLayer.configure {
            // "dependsOn" works with strings (lazy) or TaskProviders
            dependsOn(this@subprojects.tasks.named("allTests"))
        }
    }
}
