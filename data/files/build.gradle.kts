plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ksp-convention")
    kotlin("plugin.serialization")
}

val modulePackageName = "com.tunjid.heron.data.files"

android {
    namespace = modulePackageName
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":data:models"))

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.filekit.core)

                implementation(libs.okio)
            }
        }
    }
}
