plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ksp-convention")
}
android {
    namespace = "com.tunjid.heron.di"
    buildFeatures {
        compose = false
    }
}

kotlin {
    sourceSets {
        named("commonMain") {
            dependencies {
            }
        }
        named("androidMain") {
            dependencies {
            }
        }
        named("desktopMain") {
            dependencies {
            }
        }
        named("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
