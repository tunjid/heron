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

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
            export(project(":scaffold"))
            // Kotlin/Native's DevirtualizationAnalysis phase and its dependent
            // phases (BuildDFG, DCEPhase) OOM on large (>100k LOC) codebases
            // during the release link. Disabling them trades a marginally
            // larger binary / slightly slower startup for reliable release
            // builds. Revisit after Kotlin 2.4.0 stable (see KT-80367) and a
            // Compose Multiplatform release targeting it.
            if (buildType == NativeBuildType.RELEASE) {
                freeCompilerArgs += "-Xdisable-phases=DevirtualizationAnalysis,BuildDFG,DCEPhase"
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
            implementation(project(":data:logging"))
            implementation(project(":data:platform"))
            implementation(project(":scaffold"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:compose"))
            implementation(project(":feature:conversation"))
            implementation(project(":feature:edit-profile"))
            implementation(project(":feature:feed"))
            implementation(project(":feature:gallery"))
            implementation(project(":feature:graze-editor"))
            implementation(project(":feature:home"))
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
            implementation(project(":feature:splash"))
            implementation(project(":ui:media"))

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

            implementation(libs.okio)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.connectivity.http)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.tink)
        }
        iosMain.dependencies {
            api(project(":scaffold"))
            implementation(libs.connectivity.device)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.tunjid.heron.MainKt"

        buildTypes.release {
            proguard {
                version.set("7.8.0")
                configurationFiles.from(project.file("compose-desktop.pro"))
                isEnabled = false
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            packageName = "com.tunjid.heron"
            // Remove hyphenated suffixes if present
            packageVersion = scmVersion.version.split("-").first()
            outputBaseDir.set(layout.buildDirectory.dir("release"))

            // Bundle pre-built native libraries (JNA dispatch + AVFoundation) into the
            // .app package so the hardened runtime / sandbox can load them directly.
            // Run copyNativeLibsForSandbox before packaging.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            val resourcesDir = project.file("src/desktopMain/resources")
            macOS {
                bundleID = "com.tunjid.heron"
                iconFile.set(resourcesDir.resolve("icon.icns"))
                entitlementsFile.set(project.file("entitlements.plist"))
                runtimeEntitlementsFile.set(project.file("entitlements.plist"))

                providers.gradleProperty("heron.macOS.signing.identity")
                    .let { identityProperty ->
                        if (identityProperty.isPresent) signing {
                            sign.set(true)
                            identity.set(identityProperty)
                        }
                    }

                // The notarizeDmg task does not support the Gradle configuration
                // cache, so CI uses xcrun notarytool directly instead.
                // This block is for local testing only.
                notarization {
                    appleID.set(providers.gradleProperty("heron.macOS.notarization.appleID"))
                    password.set(providers.gradleProperty("heron.macOS.notarization.password"))
                    teamID.set(providers.gradleProperty("heron.macOS.notarization.teamID"))
                }
            }
            windows {
                iconFile.set(resourcesDir.resolve("icon.ico"))
            }
            linux {
                iconFile.set(resourcesDir.resolve("icon.png"))
            }
        }
    }
}

// Copy native libraries into app resources for sandboxed App Store builds.
// These are picked up by appResourcesRootDir and bundled into the .app package,
// accessible at runtime via the compose.application.resources.dir system property.
// The build and extraction tasks live in :ui:media; this module just copies the outputs.
val copyNativeLibsTasks = listOf(
    "Arm" to ("aarch64" to "macos-arm64"),
    "X64" to ("x86-64" to "macos-x86-64"),
).map { (taskSuffix, archPair) ->
    val (buildArch, resourceArch) = archPair
    tasks.register<Copy>("copyNativeLibs${resourceArch.replace("-", "")}") {
        from(project(":ui:media").layout.buildDirectory.dir("native-libs/darwin-$buildArch"))
        include("libAVFoundationVideoPlayer.dylib", "libjnidispatch.jnilib")
        into(project.file("resources/$resourceArch"))
        dependsOn(
            ":ui:media:buildAVFoundationMac$taskSuffix",
            ":ui:media:extractJnaNative$taskSuffix",
        )
    }
}

val copyNativeLibsForSandbox = tasks.register("copyNativeLibsForSandbox") {
    dependsOn(copyNativeLibsTasks)
}

// Sign native libraries with Developer ID so they pass notarization.
// Must run after copying but before packaging.
val signingIdentityProperty = providers.gradleProperty("heron.macOS.signing.identity")
val nativeLibsResourcesDir = layout.projectDirectory.dir("resources")
val signNativeLibsForSandbox = tasks.register<SignNativeLibsTask>("signNativeLibsForSandbox") {
    dependsOn(copyNativeLibsForSandbox)
    libraries.from(
        nativeLibsResourcesDir.asFileTree.matching {
            include("**/*.dylib", "**/*.jnilib")
        },
    )
    signingIdentity.set(signingIdentityProperty)
}

val nativeLibDependentTasks = setOf(
    "packageDmg",
    "packageReleaseDmg",
    "prepareAppResources",
)
tasks.matching { it.name in nativeLibDependentTasks }.configureEach {
    dependsOn(signNativeLibsForSandbox)
}

configurations {
    getByName("desktopMainApi").exclude(
        group = "org.jetbrains.kotlinx",
        module = "kotlinx-coroutines-android",
    )
}
