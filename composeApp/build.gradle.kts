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

import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("android-application-convention")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.axionRelease)
    id("ksp-convention")
    id("kotlin-jvm-convention")
}

scmVersion {
    tag {
        // Use an empty string for prefix
        prefix.set("")
    }
    repository {
        pushTagsOnly.set(true)
    }
    providers.gradleProperty("heron.releaseBranch")
        .orNull
        ?.let { releaseBranch ->
            when {
                releaseBranch.contains("bugfix/") -> versionIncrementer("incrementPatch")
                releaseBranch.contains("feature/") -> versionIncrementer("incrementMinor")
                releaseBranch.contains("release/") -> versionIncrementer("incrementMajor")
                else -> throw IllegalArgumentException("Unknown release type")
            }
        }
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.connectivity.device)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
        }
        commonMain.dependencies {
            implementation(project(":data:models"))
            implementation(project(":data:database"))
            implementation(project(":data:core"))
            implementation(project(":scaffold"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:compose"))
            implementation(project(":feature:conversation"))
            implementation(project(":feature:edit-profile"))
            implementation(project(":feature:feed"))
            implementation(project(":feature:gallery"))
            implementation(project(":feature:home"))
            implementation(project(":feature:list"))
            implementation(project(":feature:messages"))
            implementation(project(":feature:moderation"))
            implementation(project(":feature:notifications"))
            implementation(project(":feature:post-detail"))
            implementation(project(":feature:posts"))
            implementation(project(":feature:profile"))
            implementation(project(":feature:profile-avatar"))
            implementation(project(":feature:profiles"))
            implementation(project(":feature:search"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:splash"))
            implementation(project(":ui:media"))

            implementation(libs.androidx.room.runtime)

            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation.foundation)
            implementation(libs.compose.multiplatform.material)
            implementation(libs.compose.multiplatform.ui.ui)

            implementation(libs.connectivity.core)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.lifecycle.multiplatform.viewmodel)
            implementation(libs.lifecycle.multiplatform.runtime.compose)

            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.okio)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.connectivity.http)
            implementation(libs.kotlinx.coroutines.swing)
        }
        iosMain.dependencies {
            implementation(libs.connectivity.device)
        }
    }
}

android {
    namespace = "com.tunjid.heron"

    defaultConfig {
        applicationId = "com.tunjid.heron"
        versionCode = providers.gradleProperty("heron.versionCode")
            .get()
            .toInt()
        versionName = scmVersion.version
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    val releaseSigning = if (file("debugKeystore.properties").exists()) {
        signingConfigs.create("release") {
            val props = Properties()
            props.load(FileInputStream(file("debugKeystore.properties")))
            storeFile = file(props["keystore"] as String)
            storePassword = props["keystore.password"] as String
            keyAlias = props["keyAlias"] as String
            keyPassword = props["keyPassword"] as String
        }
    } else {
        signingConfigs["debug"]
    }
    buildTypes {
        all {
            signingConfig = releaseSigning
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.tunjid.heron.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.tunjid.heron"
            // Remove hyphenenated suffixes if present
            packageVersion = scmVersion.version.split("-").first()
        }
    }
}

configurations {
    getByName("desktopMainApi").exclude(
        group = "org.jetbrains.kotlinx",
        module = "kotlinx-coroutines-android",
    )
}
