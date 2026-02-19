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

import com.android.build.api.dsl.CommonExtension
import ext.ProjectJavaVersion
import ext.configureKotlinJvm
import ext.libs
import org.gradle.kotlin.dsl.dependencies

/** Sets common values for Android Applications and Libraries */
fun org.gradle.api.Project.commonConfiguration(extension: CommonExtension<*, *, *, *, *, *>) =
    extension.apply {
        compileSdk = 36

        defaultConfig {
            // The app uses Modifier.blur Which is Android 12 and up
            minSdk = 31
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = true
            sourceCompatibility = ProjectJavaVersion
            targetCompatibility = ProjectJavaVersion
        }
        configureKotlinJvm()
    }

fun org.gradle.api.Project.addDesugarDependencies() {
    dependencies {
        add(
            configurationName = "coreLibraryDesugaring",
            dependencyNotation = libs.android.desugarJdkLibs,
        )
    }
}
