/*
 * Copyright 2024 Adetunji Dahunsi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ext.configureKotlinJvm
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import java.util.Locale

fun Project.configureKotlinMultiplatform(
    kotlinMultiplatformExtension: KotlinMultiplatformExtension
) {
    val project = this@configureKotlinMultiplatform

    kotlinMultiplatformExtension.apply{
        androidTarget()
        jvm("desktop")
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        )
        sourceSets.apply {
            all {
                languageSettings.apply {
                    optIn("androidx.compose.animation.ExperimentalAnimationApi")
                    optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                    optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                    optIn("androidx.compose.material.ExperimentalMaterialApi")
                    optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                    optIn("kotlinx.serialization.ExperimentalSerializationApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("kotlinx.coroutines.FlowPreview")
                }
            }
            val catalogs = extensions.getByType(VersionCatalogsExtension::class.java)
            val libs = catalogs.named("libs")

            named("commonMain") {
//                kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
                dependencies {
                    api(libs.findLibrary("kotlin-inject-runtime").get())
                    implementation(libs.findLibrary("compose-runtime").get())
                }
            }
        }
        configureKotlinJvm()
//        tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
//            if (name != "kspCommonMainKotlinMetadata") {
//                dependsOn("kspCommonMainKotlinMetadata")
//            }
//            kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
//        }
        targets.configureEach {
            configureKsp(project = project)
        }
    }
}

private fun KotlinTarget.configureKsp(project: Project) {

    println("Target name: $targetName. Metadata? ${targetName == "metadata"}")
    if (targetName != "metadata") {
        project.dependencies {
            add(
                configurationName = "ksp${
                    targetName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    }
                }",
                dependencyNotation = project.versionCatalog.findLibrary("kotlin-inject-compiler")
                    .get()
            )
        }
    }

//    compilations.configureEach {
//        kotlinSourceSets.forEach { sourceSet ->
//            println("Target name: $targetName. sourceSet name: ${sourceSet.name}")
//
//            sourceSet.kotlin.srcDir("build/generated/ksp/$targetName/${sourceSet.name}/kotlin")
//        }
//    }
}
