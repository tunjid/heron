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

import java.util.Properties

plugins {
    id("android-application-convention")
    id("release-convention")
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
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
    val releaseSigning = when {
        // Do not sign the build output, it will be signed on CI
        providers.gradleProperty("heron.isRelease").orNull.toBoolean() -> null
        file("debugKeystore.properties").exists() -> signingConfigs.create("release") {
            val props = Properties()
            file("debugKeystore.properties")
                .inputStream()
                .use(props::load)
            storeFile = file(props.getProperty("keystore"))
            storePassword = props.getProperty("keystore.password")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
        else -> signingConfigs["debug"]
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
    implementation(project(":composeApp"))
    implementation(project(":data:models"))
    implementation(project(":data:database"))
    implementation(project(":data:core"))
    implementation(project(":data:logging"))
    implementation(project(":data:platform"))
    implementation(project(":scaffold"))

    implementation(libs.compose.multiplatform.components.resources)
    implementation(libs.compose.multiplatform.runtime)
    implementation(libs.compose.multiplatform.foundation.foundation)
    implementation(libs.compose.multiplatform.material)
    implementation(libs.compose.multiplatform.ui.ui)
    implementation(libs.compose.multiplatform.ui.tooling.preview)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    debugImplementation(libs.compose.multiplatform.ui.tooling.preview)
}
