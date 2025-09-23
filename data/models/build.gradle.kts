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

plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ksp-convention")
    id("app.cash.burst")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.heron.data"
}

kotlin {
    sourceSets {

        commonMain {
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.ktor.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.burst)
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

dependencies {
    // For Android JUnit runner to see Burst
    testImplementation(libs.burst)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "burst")
    }
    // This property tells JUnit Platform to exclude matching classes from discovery
    // which should prevent JUnit Jupiter from trying to initialize them.
    systemProperty(
        "junit.platform.discovery.exclude.classnames",
        // Regex to match classes in 'com.tunjid.heron.models.polymorphic' and subpackages
        // that end with 'SerializationTest'
        "com\\.tunjid\\.heron\\.models\\.polymorphic\\..SerializationTest*",
    )
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
