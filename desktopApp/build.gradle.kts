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

import org.gradle.api.attributes.Attribute
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
    implementation(project(":ui:scaffold"))

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

            // The Compose default jlink image ships only a minimal module set, which omits modules
            // the app references — notably jdk.management (+ java.management, pulled in transitively),
            // needed by the inference MemoryMonitor's ManagementFactory / OperatingSystemMXBean and
            // reached at startup via createAppState(); plus java.sql, jdk.unsupported, etc. Missing
            // modules only crash the packaged app, never `./gradlew run` (full JDK). Most of this
            // list is jdeps' output from `./gradlew :desktopApp:suggestModules` — re-run it after
            // dependency changes. java.net.http is added on top: ktor's Java client engine needs it
            // but is selected via ServiceLoader (reflection), so jdeps can't see it and omits it.
            modules(
                "java.compiler",
                "java.instrument",
                "java.net.http",
                "java.sql",
                "jdk.jfr",
                "jdk.management",
                "jdk.security.auth",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
            )

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

// Copy native libraries into app resources for the sandboxed arm64 macOS build.
// These are picked up by appResourcesRootDir and bundled into the .app package,
// accessible at runtime via the compose.application.resources.dir system property.
// The build and extraction tasks live in :ui:media and :data:ml; this module just
// copies the outputs. macOS ships arm64 only.
val copyNativeLibsForSandbox = tasks.register<Copy>("copyNativeLibsForSandbox") {
    into(project.file("resources/macos-arm64"))

    from(project(":ui:media").layout.buildDirectory.dir("native-libs/darwin-aarch64")) {
        include("libAVFoundationVideoPlayer.dylib", "libjnidispatch.jnilib")
    }
    from(project(":data:ml").layout.buildDirectory.dir("native-libs/darwin-aarch64")) {
        include("liblitertlm_jni.so")
    }

    dependsOn(
        ":ui:media:buildAVFoundationMacArm",
        ":ui:media:extractJnaNativeArm",
        ":data:ml:extractLitertlmNativeArm",
    )
}

// Sign native libraries with Developer ID so they pass notarization.
// Must run after copying but before packaging.
val signingIdentityProperty = providers.gradleProperty("heron.macOS.signing.identity")
val nativeLibsResourcesDir = layout.projectDirectory.dir("resources")
val signNativeLibsForSandbox = tasks.register<SignNativeLibsTask>("signNativeLibsForSandbox") {
    dependsOn(copyNativeLibsForSandbox)
    libraries.from(
        nativeLibsResourcesDir.asFileTree.matching {
            include("**/*.dylib", "**/*.jnilib", "**/*.so")
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

// Strip the ad-hoc-signed macOS native from the litertlm-jvm JAR before Compose bundles it into the
// signed .app. Apple's notary service scans inside JARs; that in-JAR copy of liblitertlm_jni.so has
// no Developer ID / secure timestamp and fails notarization. The app loads a signed copy from
// resources instead (see main.kt + StripLitertlmMacNativeTransform), so the in-JAR copy is dead
// weight in the packaged app.
//
// Gated to signed DMG/distributable builds: only when a signing identity is configured AND a
// packaging task was requested. A bare `./gradlew run` requests neither, so local on-device ML
// keeps the native. The strip must happen on the classpath (before jpackage assembles and signs
// the app), which is why this is an artifact transform rather than a post-package step.
val packagingDistributable = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("dmg", ignoreCase = true) ||
        taskName.contains("distributable", ignoreCase = true)
}
if (signingIdentityProperty.isPresent && packagingDistributable) {
    val litertlmMacNativeStripped =
        Attribute.of("heron.litertlm.macNativeStripped", Boolean::class.javaObjectType)
    dependencies {
        attributesSchema {
            attribute(litertlmMacNativeStripped)
        }
        artifactTypes.getByName("jar").attributes.attribute(litertlmMacNativeStripped, false)
        registerTransform(StripLitertlmMacNativeTransform::class.java) {
            from.attribute(litertlmMacNativeStripped, false)
            to.attribute(litertlmMacNativeStripped, true)
        }
    }
    configurations.named("runtimeClasspath") {
        attributes.attribute(litertlmMacNativeStripped, true)
    }
}
