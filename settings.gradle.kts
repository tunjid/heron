import java.net.URI

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
        // google().mavenContent { … } (not google { … }) so Dependabot's repository parser,
        // which matches only the `google(` call form, detects Google Maven and can resolve the
        // androidx / com.android / com.google artifacts. Resolution behavior is unchanged.
        google().mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // google().mavenContent { … } (not google { … }) so Dependabot's repository parser,
        // which matches only the `google(` call form, detects Google Maven and can resolve the
        // androidx / com.android / com.google artifacts. Resolution behavior is unchanged.
        google().mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
        mavenCentral()
    }
}

include(
    ":androidApp",
    ":composeApp",
    ":desktopApp",
    ":data:core",
    ":data:files",
    ":data:graze",
    ":data:logging",
    ":data:ml",
    ":data:platform",
    ":data:models",
    ":data:database",
    ":data:lexicons",
    ":data:tasks",
    ":ui:tiling",
    ":feature:atmosphereapp",
    ":feature:auth",
    ":feature:compose",
    ":feature:conversation",
    ":feature:edit-profile",
    ":feature:feed",
    ":feature:gallery",
    ":feature:graze-editor",
    ":feature:home",
    ":feature:inference",
    ":feature:list",
    ":feature:messages",
    ":feature:moderation",
    ":feature:notifications",
    ":feature:notification-settings",
    ":feature:post-detail",
    ":feature:posts",
    ":feature:profile",
    ":feature:profile-avatar",
    ":feature:profiles",
    ":feature:search",
    ":feature:splash",
    ":feature:settings",
    ":feature:standard-publication",
    ":feature:standard-subscription",
    ":feature:tasks",
    ":feature:template",
    ":ui:core",
    ":ui:media",
    ":ui:preview",
    ":ui:profile",
    ":ui:scaffold",
    ":ui:sheets",
    ":ui:tiling",
    ":ui:timeline",
)

// The app modules physically live under apps/ but keep their original logical project
// paths (:androidApp, :composeApp, :desktopApp) so task paths and project(...) references
// stay unchanged — only the projectDir is relocated.
project(":androidApp").projectDir = file("apps/androidApp")
project(":composeApp").projectDir = file("apps/composeApp")
project(":desktopApp").projectDir = file("apps/desktopApp")
