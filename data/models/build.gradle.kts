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
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.heron.data"
}

kotlin {
    sourceSets {
        val commonTest = getByName("commonTest")

        val desktopTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
                implementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
            }
        }

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
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

// Bridge desktopTest - jvmTest so that IDE/Gradle can run it
tasks.register("jvmTest") {
    dependsOn("desktopTest")
}
tasks.register("cleanJvmTest") {
    dependsOn("cleanDesktopTest")
}
