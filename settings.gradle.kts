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
    ":domain-timeline",
    ":scaffold",
    ":feature-auth",
    ":feature-compose",
    ":feature-feed",
    ":feature-home",
    ":feature-messages",
    ":feature-notifications",
    ":feature-post-detail",
    ":feature-profile",
    ":feature-profiles",
    ":feature-search",
    ":feature-splash",
    ":feature-template",
    ":ui-timeline",
    ":ui-images",
)
