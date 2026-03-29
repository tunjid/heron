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

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class SignNativeLibsTask : DefaultTask() {
    @get:InputFiles abstract val libraries: ConfigurableFileCollection

    @get:Input @get:Optional abstract val signingIdentity: Property<String>

    @get:Inject abstract val execOps: ExecOperations

    @TaskAction
    fun sign() {
        if (libraries.isEmpty || !signingIdentity.isPresent) return
        val identity = signingIdentity.get()
        val paths = libraries.files.map { it.absolutePath }
        execOps.exec {
            commandLine("codesign", "--force", "--timestamp", "--sign", identity, *paths.toTypedArray())
        }
    }
}
