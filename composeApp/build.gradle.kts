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

import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("kotlin-library-convention")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("release-convention")
    id("ksp-convention")
    id("kotlin-jvm-convention")
}

kotlin {
    androidLibrary {
        namespace = "com.tunjid.heron.app"
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(project(":ui:scaffold"))
            export(project(":data:ml"))
            // Kotlin/Native's DevirtualizationAnalysis phase OOMs on large
            // (>100k LOC) codebases during the release link — the memory is
            // consumed by ConstraintGraphBuilder. We also disable the
            // downstream Devirtualization phase which would otherwise crash
            // trying to apply missing analysis results. BuildDFG, DCEPhase,
            // EscapeAnalysis and the rest still run normally, so we only
            // lose devirtualization as an optimization (minor perf cost,
            // marginally larger binary). Revisit after Kotlin 2.4.0 stable
            // (see KT-80367) and a Compose Multiplatform release targeting it.
            if (buildType == NativeBuildType.RELEASE) {
                freeCompilerArgs += "-Xdisable-phases=DevirtualizationAnalysis,Devirtualization"
            }
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.connectivity.device)
        }
        commonMain.dependencies {
            implementation(project(":data:models"))
            implementation(project(":data:database"))
            implementation(project(":data:core"))
            implementation(project(":data:files"))
            implementation(project(":data:logging"))
            implementation(project(":data:ml"))
            implementation(project(":data:platform"))
            implementation(project(":data:tasks"))
            implementation(project(":ui:scaffold"))
            implementation(project(":feature:atmosphereapp"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:compose"))
            implementation(project(":feature:conversation"))
            implementation(project(":feature:edit-profile"))
            implementation(project(":feature:feed"))
            implementation(project(":feature:gallery"))
            implementation(project(":feature:graze-editor"))
            implementation(project(":feature:home"))
            implementation(project(":feature:inference"))
            implementation(project(":feature:list"))
            implementation(project(":feature:messages"))
            implementation(project(":feature:moderation"))
            implementation(project(":feature:notifications"))
            implementation(project(":feature:notification-settings"))
            implementation(project(":feature:post-detail"))
            implementation(project(":feature:posts"))
            implementation(project(":feature:profile"))
            implementation(project(":feature:profile-avatar"))
            implementation(project(":feature:profiles"))
            implementation(project(":feature:search"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:standard-publication"))
            implementation(project(":feature:standard-subscription"))
            implementation(project(":feature:tasks"))
            implementation(project(":feature:splash"))
            implementation(project(":ui:core"))
            implementation(project(":ui:media"))
            implementation(project(":ui:sheets"))
            implementation(project(":ui:timeline"))

            implementation(libs.androidx.room.runtime)

            implementation(libs.compose.multiplatform.components.resources)
            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation.foundation)
            implementation(libs.compose.multiplatform.material)
            implementation(libs.compose.multiplatform.ui.ui)
            implementation(libs.compose.multiplatform.ui.tooling.preview)

            implementation(libs.connectivity.core)

            implementation(libs.lifecycle.multiplatform.viewmodel)
            implementation(libs.lifecycle.multiplatform.runtime.compose)

            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.core)

            implementation(libs.okio)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.connectivity.http)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.tink)
        }
        iosMain.dependencies {
            api(project(":ui:scaffold"))
            api(project(":data:ml"))
            implementation(libs.connectivity.device)
        }
    }
}

configurations {
    getByName("desktopMainApi").exclude(
        group = "org.jetbrains.kotlinx",
        module = "kotlinx-coroutines-android",
    )
}
