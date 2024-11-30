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

import com.android.build.api.dsl.CommonExtension
import ext.configureKotlinJvm
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies

/**
 * Sets common values for Android Applications and Libraries
 */
fun org.gradle.api.Project.commonConfiguration(
    extension: CommonExtension<*, *, *, *, *, *>
) = extension.apply {
    compileSdk = 35

    defaultConfig {
        // Could have been 21, but I need sqlite 3.24.0 for upserts
        minSdk = 30
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    configureKotlinJvm()
}

fun org.gradle.api.Project.addDesugarDependencies() {
    dependencies {
        add(
            configurationName = "coreLibraryDesugaring",
            dependencyNotation = versionCatalog.findLibrary("android-desugarJdkLibs").get()
        )
    }
}

val org.gradle.api.Project.versionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java)
        .named("libs")