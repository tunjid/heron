import com.android.build.gradle.internal.ide.kmp.KotlinAndroidSourceSetMarker.Companion.android
import java.util.zip.ZipFile

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
    id("org.jetbrains.compose")
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.tunjid.heron.ui.images"
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid") {
                withJvm()
                withIosArm64()
                withIosSimulatorArm64()
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":data:files"))
                implementation(project(":data:logging"))
                implementation(project(":data:models"))
                implementation(project(":ui:core"))

                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.components.resources)
                implementation(libs.compose.multiplatform.foundation.layout)
                implementation(libs.compose.multiplatform.foundation.foundation)
                implementation(libs.compose.multiplatform.material.icons.extended)
                implementation(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.runtime)
                implementation(libs.compose.multiplatform.ui.ui)

                implementation(libs.coil.compose)
                implementation(libs.coil.ktor)

                implementation(libs.filekit.coil)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                implementation(libs.tunjid.composables)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.android)
                implementation(libs.androidx.media3.datasource.okhttp)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.exoplayer.dash)
                implementation(libs.androidx.media3.exoplayer.hls)

                implementation(libs.andrew.bailey.difference)
                implementation(libs.coil.gif.android)
            }
        }
        val nonAndroidMain by getting {
            dependencies {
                implementation(libs.coil.compose)
                implementation(libs.skiko)
            }
        }
        desktopMain {
            dependencies {
                implementation(libs.ktor.client.java)
                implementation(libs.jna)
                implementation(libs.jna.platform)
                implementation(libs.gstreamer.java.core)

                val osName = System.getProperty("os.name")
                val fxClassifier = when {
                    osName.startsWith("Mac OS X") ->
                        if (System.getProperty("os.arch") == "aarch64") "mac-aarch64" else "mac"
                    osName.startsWith("Linux") -> "linux"
                    else -> "win"
                }
                implementation("${libs.javafx.base.get().module}:${libs.versions.javafx.get()}:$fxClassifier")
                implementation("${libs.javafx.media.get().module}:${libs.versions.javafx.get()}:$fxClassifier")
                implementation("${libs.javafx.swing.get().module}:${libs.versions.javafx.get()}:$fxClassifier")
                implementation("${libs.javafx.graphics.get().module}:${libs.versions.javafx.get()}:$fxClassifier")
            }
        }
        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

val macTargets = listOf(
    Triple("Arm", "arm64-apple-macosx14.0", "aarch64"),
    Triple("X64", "x86_64-apple-macosx14.0", "x86-64"),
)

val swiftSourceFile = file("src/desktopMain/swift/AVFoundationVideoPlayer.swift")

macTargets.forEach { (name, target, arch) ->
    val outputDir = layout.buildDirectory.dir("native-libs/darwin-$arch")
    val dylibFile = layout.buildDirectory.file("native-libs/darwin-$arch/libAVFoundationVideoPlayer.dylib")
    tasks.register<Exec>("buildAVFoundationMac$name") {
        onlyIf { System.getProperty("os.name").startsWith("Mac") }
        inputs.file(swiftSourceFile)
        outputs.file(dylibFile)
        workingDir(rootDir)
        commandLine(
            "swiftc", "-emit-library", "-emit-module", "-module-name", "AVFoundationVideoPlayer",
            "-target", target,
            "-o", dylibFile.get().asFile.absolutePath,
            swiftSourceFile.absolutePath,
            "-O", "-whole-module-optimization",
        )
    }
}

// Add the native-libs build output as a resource directory so desktopProcessResources
// picks up the dylib and jnilib for development runs (IDE run configurations).
kotlin.sourceSets.named("desktopMain") {
    resources.srcDir(layout.buildDirectory.dir("native-libs"))
}

tasks.named("desktopProcessResources") {
    if (System.getProperty("os.name").startsWith("Mac")) {
        dependsOn(
            "buildAVFoundationMacArm",
            "buildAVFoundationMacX64",
            "extractJnaNativeArm",
            "extractJnaNativeX64",
        )
    }
}

// Extract JNA's native dispatch library from the JNA JAR for sandboxed App Store builds.
// The extracted libjnidispatch.jnilib is placed alongside the AVFoundation dylib so that
// composeApp can copy both into appResourcesRootDir.
val jnaJar = configurations.named("desktopRuntimeClasspath").map { config ->
    config.files.first { it.name.startsWith("jna-") && !it.name.contains("platform") }
}

macTargets.forEach { (name, _, arch) ->
    tasks.register("extractJnaNative$name") {
        val outputDir = layout.buildDirectory.dir("native-libs/darwin-$arch")
        val jnaJarFile = jnaJar
        inputs.files(jnaJarFile)
        outputs.file(outputDir.map { it.file("libjnidispatch.jnilib") })
        doLast {
            val jarFile = jnaJarFile.get()
            val entryPath = "com/sun/jna/darwin-$arch/libjnidispatch.jnilib"
            val dir = outputDir.get().asFile
            val outputFile = dir.resolve("libjnidispatch.jnilib")
            ZipFile(jarFile).use { zip ->
                val entry = zip.getEntry(entryPath)
                    ?: error("Entry $entryPath not found in ${jarFile.name}")
                zip.getInputStream(entry).use { stream ->
                    outputFile.outputStream().use { out -> stream.copyTo(out) }
                }
            }
        }
    }
}

fun osName() = System.getProperty("os.name")
