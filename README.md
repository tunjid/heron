# Heron

Heron is a Jetpack Compose adaptive, reactive and offline-first Bluesky client.

## Download
<a href="https://play.google.com/store/apps/details?id=com.tunjid.heron&referrer=utm_campaign%3Dandroid_metadata%26utm_medium%3Dweb%26utm_source%3Dgithub.com%26utm_content%3Dbadge" target="_blank"><img src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg" alt="Get it on Google Play" height="48"></a>

## Screenshots

| ![Scroll animations](./docs/images/1.gif) | ![Screen transitions](./docs/images/2.gif) | ![Thread diving](./docs/images/3.gif) |
|-------------------------------------------|--------------------------------------------|---------------------------------------|

## UI/UX and App Design

Heron uses [Material design and motion](https://m3.material.io/) and is heavily inspired by the
[Crane](https://m2.material.io/design/material-studies/crane.html) material study.

Libraries:

1. Geometric shapes are created with
   the [Jetpack shapes](https://developer.android.com/jetpack/androidx/releases/graphics) graphics
   library.
2. UX patterns like pinch to zoom, drag to dismiss, collapsing headers, multipane layouts, and
   and so on are implemented with the [Composables](https://github.com/tunjid/composables) library.

## Architecture

This is a
multi-module [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
project targeting Android, iOS and Desktop that follows
the [Android architecture guide](https://developer.android.com/topic/architecture).

For more details about this kind of architecture, take a look at
the [Now in Android sample](https://github.com/android/nowinandroid) repository,
this app follows the same architecture principles it does, and the architecture decisions are very
similar.

There are 6 kinds of modules:

1. `data-*` is the [data layer](https://developer.android.com/topic/architecture/data-layer) of the
   app containing models data and repository implementations for reading and writing that data.
   Data reads should never error, while writes are queued with a `WriteQueue`.
    - [Jetpack Room](https://developer.android.com/jetpack/androidx/releases/room)
      is used for persisting data with SQLite.
        - Given the highly relational nature of the app, a class called a `MultipleEntitySaver` is
          used to save bluesky
          network models.
    - [Jetpack DataStore](https://developer.android.com/jetpack/androidx/releases/datastore)
      is used for blob storage of arbitrary data with protobufs.
    - [Ktor](https://ktor.io/) is used for network connections via the
      [Ozone at-proto bindings](https://github.com/christiandeange/ozone).
2. `domain-*` is the [domain layer](https://developer.android.com/topic/architecture/domain-layer)
   where aggregated business logic lives. This is mostly `domain-timeline` where higher level
   abstractions timeline data manipulation exists.
3. `feature-*` contains navigation destinations or screens in the app. Multiple features can
   run side by side in app panes depending on the navigation configuration and device screen size.
4. `ui-*` contains standalone and reusable UI components and Jetpack Compose effects for the app
   ranging from basic layout to multimedia components for displaying photos and video playback.
5. `scaffold` contains the application state in the `AppState` class, and coordinates app level
   UI logic like pane display, drag to dismiss, back previews and so on. It is the entry point to
   the multiplatform application
6. `/composeApp` is app module that is the fully assembled app and depends on all other modules.
   It offers the entry point to the application for a platform.
   It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the
      folder name.
    - `/iosApp` is the entry point for the iOS app.

### Dependency Injection

Dependency injection is implemented with the [Metro](https://github.com/ZacSweers/metro)
library which constructs the dependency graph at build time
and therefore is compile time safe. Assisted injection is used for feature screens to pass
navigation arguments information to the feature. The items in the dependency graph are:

* `NavigationBindings` from feature modules providing Navigation3 `NavEntry` instances per feature.
* Feature `Bindings` from feature modules providing access to the data layer and app scaffold to each module.
* Scaffold `Bindings` providing the `PaneScaffoldState` for building a multi-pane app,
* Data `Bindings` for the data layer.
* An `AppNavigationGraph` for resolving navigation routes.
* An `AppGraph` containing the entire app DI graph.

### Navigation

Navigation uses the [treenav experiment](https://github.com/tunjid/treeNav) to implement
Android [adaptive navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive).
Specifically it uses a `ThreePane` configuration, where up to 3 navigation panes may be shown, with
one reserved for back previews and another for modals. Navigation state is also saved to disk and
persisted across app restarts.

### State production

* State production follows
  the [Android guide to UI State Production](https://developer.android.com/topic/architecture/ui-layer/state-production).
* Each feature uses a
  single [Jetpack ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
  as
  the [business logic state holder](https://developer.android.com/topic/architecture/ui-layer/stateholders).
* State is produced in a lifecycle aware way using
  the [Jetpack Lifecyle](https://developer.android.com/jetpack/androidx/releases/lifecycle) APIs.
    * The `CoroutineScope` for each `ViewModel` is obtained from the composition's
      `LocalLifecycleOwner`
* The specifics of producing state over time is implemented with
  the [Mutator library](https://github.com/tunjid/Mutator).
    * Inputs to the state production pipeline are passed to the mutator in the `inputs` argument, or
      derived from an action in `actionTransform`.
    * Every coroutine launched is limited to running when the lifecycle of the component displaying
      it is resumed. When the lifecyle
      is paused, the coroutines are cancelled after 2 seconds:
      `SharingStarted.WhileSubscribed(FeatureWhileSubscribed)`.
    * Each user `Action` is in a sealed hierarchy, and action parallelism is defined by the
      `Action.key`. Actions with different keys run in parallel
      while those in the same key are processed sequentially. Each distinct subtype of an `Action`
      hierarchy typically has it's own
      key unless sequential processing is required for example:
        * All subtypes of `Action.Navigation` typically share the same key.
        * All subtypes of pagination actions, also share the same key and are processed with
          the [Tiling library](https://github.com/tunjid/Tiler).

## Building

### iOS Push Notifications

iOS push notifications are powered by Firebase Cloud Messaging (FCM) via Apple Push Notification
service (APNs). The following setup is required:

1. **Firebase iOS SDK** - Added via Swift Package Manager in `iosApp/iosApp.xcodeproj`.
   The `FirebaseMessaging` package is required.

2. **`GoogleService-Info.plist`** - Download from Firebase Console > Project Settings > your iOS app
   and place at `iosApp/iosApp/GoogleService-Info.plist`. This file is `.gitignore`d; in CI, decode
   it from a base64 secret:
   ```bash
   echo "$GOOGLE_SERVICE_INFO_PLIST_BASE64" | base64 -d > iosApp/iosApp/GoogleService-Info.plist
   ```

3. **Push Notifications capability** - Enabled in Xcode under Signing & Capabilities. This adds
   `aps-environment` to `iosApp.entitlements`.

4. **Background Modes capability** - Enabled in Xcode with **Remote notifications** checked. This
   adds `remote-notification` to `UIBackgroundModes` in `Info.plist`, which is required for
   data-only/silent push delivery.

5. **APNs Authentication Key** - Create a `.p8` key in the
   [Apple Developer Portal](https://developer.apple.com/account/resources/authkeys/list)
   under Keys with Apple Push Notifications enabled. Note the Key ID and Team ID.

6. **Upload APNs key to Firebase** - In Firebase Console > Project Settings > Cloud Messaging >
   iOS app, upload the `.p8` key along with the Key ID and Team ID.

7. **Backend payload format** - FCM payloads targeting iOS must include `content-available: 1`
   in `apns.payload.aps` for data-only push delivery. Without this, iOS silently drops the
   notification. Note that iOS throttles silent pushes and does not deliver them to
   force-quit apps.

### Gradle Properties

The following properties can be set in `~/.gradle/gradle.properties` or passed via `-P` flags.
None are required for basic development builds.

| Property | Description                                                                                                                                                           |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `heron.versionCode` | Integer version code. Managed by CI via `github.run_number`.                                                                                                          |
| `heron.endpoint` | Backend endpoint URL for the app.                                                                                                                                     |
| `heron.isRelease` | Set to `true` when building release artifacts.                                                                                                                        |
| `heron.releaseBranch` | Branch prefix (`bugfix/`, `feature/`, `release/`) controlling version increments.                                                                                     |
| `heron.macOS.signing.identity` | Name of the Developer ID Application certificate in your Keychain (e.g. `Developer ID Application: Name (TEAM_ID)`). When present, the macOS DMG will be code signed. |
macOS signing is only configured when `heron.macOS.signing.identity` is present,
so contributors without an Apple Developer account can still build unsigned DMGs with
`./gradlew packageReleaseDmg`.

Notarization is handled externally via `xcrun notarytool` (not a Gradle task) to maintain
compatibility with the Gradle configuration cache. To notarize locally after building a signed DMG:

```bash
./gradlew packageReleaseDmg
xcrun notarytool submit <path-to-dmg> \
  --apple-id <your-apple-id> \
  --password <app-specific-password> \
  --team-id <team-id> \
  --wait
xcrun stapler staple <path-to-dmg>
```

### Publishing

Publishing is triggered manually via the `Publish` GitHub Actions workflow (`workflow_dispatch`).
It runs two jobs in parallel:

**Android** (`publish-android-app`) builds a release AAB, signs it, uploads to the Play Store
internal track, extracts a universal APK, and attaches it to a draft GitHub Release.

**macOS** (`publish-mac-app`) imports a signing certificate, builds a signed DMG
via `packageReleaseDmg`, notarizes it with `xcrun notarytool`, staples the ticket,
and attaches it to the same draft GitHub Release.

The following repository secrets are required for CI publishing:

| Secret | Used by |
|---|---|
| `HERON_ENDPOINT` | Both jobs |
| `GOOGLE_SERVICES_BASE_64` | Android |
| `SIGNING_KEY_BASE_64` | Android |
| `ALIAS` | Android |
| `KEY_STORE_PASSWORD` | Android |
| `KEY_PASSWORD` | Android |
| `MACOS_SIGNING_CERTIFICATE_P12_DATA` | macOS - base64-encoded Developer ID Application `.p12` file |
| `MACOS_SIGNING_CERTIFICATE_PASSWORD` | macOS - password for the `.p12` file |
| `MACOS_SIGNING_IDENTITY` | macOS - certificate identity string (e.g. `Developer ID Application: Name (TEAM_ID)`) |
| `MACOS_NOTARIZATION_APPLE_ID` | macOS - Apple ID email |
| `MACOS_NOTARIZATION_PASSWORD` | macOS - app-specific password |
| `MACOS_NOTARIZATION_TEAM_ID` | macOS - Apple Developer Team ID |
