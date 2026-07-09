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
    id("kotlin-library-convention")
    id("ksp-convention")
    kotlin("plugin.serialization")
}

kotlin {
    androidLibrary {
        namespace = "com.tunjid.heron.data.ml"
    }
}

kotlin {
    applyDefaultHierarchyTemplate()
    sourceSets {
        val litertMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly(libs.litertlm.jvm)
            }
        }
        commonMain {
            dependencies {
                implementation(project(":data:models"))
                implementation(project(":data:logging"))
                implementation(project(":data:tasks"))

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            dependsOn(litertMain)
            dependencies {
                implementation(libs.litertlm.android)
            }
        }
        desktopMain {
            dependsOn(litertMain)
            dependencies {
                implementation(libs.litertlm.jvm)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// Extract LiteRT-LM's macOS JNI library from the litertlm-jvm JAR so :desktopApp can copy it
// into the sandboxed .app bundle and sign it with Developer ID. litertlm ships it only ad-hoc
// signed, which fails notarization, and its NativeLibraryLoader would otherwise extract an
// unsigned copy to a temp dir at runtime (blocked by the hardened runtime / sandbox).
// arm64 only — litertlm publishes no x86-64 macOS binary. Mirrors JNA extraction in :ui:media.
val litertlmJvmJar = configurations.named("desktopRuntimeClasspath").map { config ->
    config.files
        .firstOrNull { it.name.startsWith("litertlm-jvm") }
        ?: error("litertlm-jvm JAR not found in desktopRuntimeClasspath")
}

tasks.register("extractLitertlmNativeArm") {
    val outputDir = layout.buildDirectory.dir("native-libs/darwin-aarch64")
    val jarProvider = litertlmJvmJar
    inputs.files(jarProvider)
    outputs.file(outputDir.map { it.file("liblitertlm_jni.so") })

    // Declared on all platforms to satisfy Gradle's implicit dependency detection,
    // but only executes on macOS where the signed .app bundle is produced.
    onlyIf { System.getProperty("os.name").startsWith("Mac") }

    doLast {
        val jarFile = jarProvider.get()
        val entryPath = "com/google/ai/edge/litertlm/jni/darwin-aarch64/liblitertlm_jni.so"
        val outputFile = outputDir.get().asFile.also { it.mkdirs() }.resolve("liblitertlm_jni.so")
        ZipFile(jarFile).use { zip ->
            val entry = zip.getEntry(entryPath)
                ?: error("Entry $entryPath not found in ${jarFile.name}")
            zip.getInputStream(entry).use { stream ->
                outputFile.outputStream().use { out -> stream.copyTo(out) }
            }
        }
    }
}
