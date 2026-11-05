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

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath

/**
 * Removes the macOS arm64 JNI library from the litertlm-jvm JAR before it is bundled into the
 * desktop `.app`.
 *
 * Apple's notary service recurses into bundled JARs and validates every Mach-O it finds. litertlm
 * ships `liblitertlm_jni.so` ad-hoc / linker-signed with no secure timestamp, so that in-JAR copy
 * fails notarization with "not signed with a valid Developer ID certificate" even though the loose
 * copy in the app's resources is signed properly.
 *
 * The packaged app never loads the in-JAR copy: desktopApp `main.kt` pre-loads the Developer
 * ID-signed copy from `compose.application.resources.dir`, which makes litertlm's
 * `NativeLibraryLoader` short-circuit instead of extracting the JAR copy to a temp dir. So dropping
 * it from the JAR loses nothing at runtime and unblocks notarization.
 *
 * Every other artifact is returned unchanged (the input file is registered as-is, no copy).
 */
abstract class StripLitertlmMacNativeTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    @get:Classpath
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (!input.name.startsWith("litertlm-jvm")) {
            // Not the offending JAR — pass it through untouched (returning the input avoids a copy).
            outputs.file(input)
            return
        }

        val output = outputs.file("${input.nameWithoutExtension}-macstripped.jar")
        ZipFile(input).use { zip ->
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                for (entry in zip.entries()) {
                    if (entry.name == MAC_NATIVE_ENTRY) continue
                    out.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                    if (!entry.isDirectory) {
                        zip.getInputStream(entry).use { it.copyTo(out) }
                    }
                    out.closeEntry()
                }
            }
        }
    }

    private companion object {
        const val MAC_NATIVE_ENTRY =
            "com/google/ai/edge/litertlm/jni/darwin-aarch64/liblitertlm_jni.so"
    }
}
