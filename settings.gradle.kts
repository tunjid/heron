rootProject.name = "heron"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(
    ":composeApp",
    ":data",
    ":data-core",
    ":data-database",
    ":di",
    ":scaffold",
    ":feature-auth",
    ":feature-home",
    ":feature-post-detail",
    ":feature-profile",
    ":feature-splash",
    ":feature-template",
    ":ui-feed",
    ":ui-images",
)
