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

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("android-application-convention")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("ksp-convention")
    id("kotlin-jvm-convention")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
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
            implementation(libs.androidx.activity.compose)
            implementation(libs.filekit.dialogs)
        }
        commonMain.dependencies {
            implementation(project(":data-core"))
            implementation(project(":data-database"))
            implementation(project(":data"))
            implementation(project(":scaffold"))
            implementation(project(":feature-auth"))
            implementation(project(":feature-compose"))
            implementation(project(":feature-feed"))
            implementation(project(":feature-gallery"))
            implementation(project(":feature-home"))
            implementation(project(":feature-list"))
            implementation(project(":feature-messages"))
            implementation(project(":feature-notifications"))
            implementation(project(":feature-post-detail"))
            implementation(project(":feature-profile"))
            implementation(project(":feature-profile-avatar"))
            implementation(project(":feature-profiles"))
            implementation(project(":feature-search"))
            implementation(project(":feature-splash"))
            implementation(project(":ui-media"))

            implementation(libs.androidx.room.runtime)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui.ui)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)

            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.okio)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

android {
    namespace = "com.tunjid.heron"

    defaultConfig {
        applicationId = "com.tunjid.heron"
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        getByName("debug") {
            if (file("debugKeystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(file("debugKeystore.properties")))
                storeFile = file(props["keystore"] as String)
                storePassword = props["keystore.password"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
        create("release") {
            if (file("debugKeystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(file("debugKeystore.properties")))
                storeFile = file(props["keystore"] as String)
                storePassword = props["keystore.password"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
            packageVersion = "1.0.0"
        }
    }
}

configurations {
    getByName("desktopMainApi").exclude(
        group = "org.jetbrains.kotlinx",
        module = "kotlinx-coroutines-android"
    )
}