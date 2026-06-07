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

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("release-convention")
    id("kotlin-jvm-convention")
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":scaffold"))

    implementation(compose.desktop.currentOs)
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

            val resourcesDir = project.file("src/main/resources")
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
