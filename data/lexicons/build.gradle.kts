import sh.christian.ozone.api.generator.ApiReturnType

plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ksp-convention")
    id("sh.christian.ozone.generator")
    kotlin("plugin.serialization")
}

val modulePackageName = "com.tunjid.heron.data.lexicons"

android {
    namespace = modulePackageName
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)

                implementation(libs.ktor.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.resources)
                implementation(libs.ktor.serialization.kotlinx.json)

                implementation(libs.okio)
            }
        }
    }
}

dependencies {
    lexicons(
        fileTree("schemas") {
            include("**/*.json")
        },
    )
}

lexicons {
    namespace.set(modulePackageName)
    defaults {
        generateUnknownsForSealedTypes.set(true)
        generateUnknownsForEnums.set(true)
    }

    generateApi("BlueskyApi") {
        packageName.set(modulePackageName)
        withKtorImplementation("XrpcBlueskyApi")
        returnType.set(ApiReturnType.Response)
        suspending.set(true)
    }
}
