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

rootProject.name = "heron"
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
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
    ":data:core",
    ":data:models",
    ":data:database",
    ":di",
    ":ui:tiling",
    ":scaffold",
    ":feature:auth",
    ":feature:compose",
    ":feature:conversation",
    ":feature:feed",
    ":feature:gallery",
    ":feature:home",
    ":feature:list",
    ":feature:messages",
    ":feature:notifications",
    ":feature:post-detail",
    ":feature:profile",
    ":feature:profile-avatar",
    ":feature:profiles",
    ":feature:search",
    ":feature:splash",
    ":feature:settings",
    ":feature:template",
    ":ui:core",
    ":ui:media",
    ":ui:tiling",
    ":ui:timeline",
)
