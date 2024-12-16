import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
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
        }
        commonMain.dependencies {
            implementation(project(":data"))
            implementation(project(":scaffold"))
            implementation(project(":feature-auth"))
            implementation(project(":feature-home"))
            implementation(project(":feature-post-detail"))
            implementation(project(":feature-profile"))
            implementation(project(":feature-splash"))

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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    getByName("desktopMainApi").exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
}