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

import java.io.File

plugins {
    id("kotlin-library-convention")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.valkyrie)
}

kotlin {
    androidLibrary {
        namespace = "com.tunjid.heron.ui.icons"
    }
    sourceSets {
        commonMain {
            dependencies {
                api(libs.compose.multiplatform.ui.ui)
                api(libs.compose.multiplatform.ui.graphics)
            }
        }
    }
}

valkyrie {
    packageName = "com.tunjid.heron.ui.icons"
    iconPack {
        name = "HeronIcons"
        targetSourceSet = "commonMain"
        nested {
            name = "Regular"
            sourceFolder = "regular"
        }
        nested {
            name = "AutoMirrored"
            sourceFolder = "automirrored"
            autoMirror = true
        }
    }
}

abstract class GenerateIosIconAssets : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val svgDir: DirectoryProperty

    @get:OutputDirectory
    abstract val xcassetsDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val catalog = xcassetsDir.get().asFile
        catalog.deleteRecursively()
        catalog.mkdirs()
        File(catalog, "Contents.json").writeText(
            """{"info":{"author":"xcode","version":1}}""",
        )
        svgDir.get().asFile.walk()
            .filter { it.isFile && it.extension == "svg" }
            .forEach { svg ->
                val imageSet = File(catalog, "${svg.nameWithoutExtension}.imageset")
                imageSet.mkdirs()
                svg.copyTo(File(imageSet, svg.name), overwrite = true)
                File(imageSet, "Contents.json").writeText(
                    """
                    {
                      "images" : [
                        {
                          "idiom" : "universal",
                          "filename" : "${svg.name}"
                        }
                      ],
                      "info" : { "author" : "xcode", "version" : 1 },
                      "properties" : {
                        "preserves-vector-representation" : true,
                        "template-rendering-intent" : "template"
                      }
                    }
                    """.trimIndent(),
                )
            }
    }
}

tasks.register<GenerateIosIconAssets>("generateIosIconAssets") {
    description = "Generate icons for ios from svgs defined in this module"
    svgDir.set(layout.projectDirectory.dir("src/commonMain/valkyrieResources"))
    xcassetsDir.set(
        rootProject.layout.projectDirectory.dir("apps/iosApp/iosApp/HeronIcons.xcassets"),
    )
}
