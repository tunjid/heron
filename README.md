# Heron

Heron is a Jetpack Compose adaptive, reactive and offline-first bluesky client.

## Screenshots

| ![split layouts desktop](./docs/images/portrait.gif) | ![split layouts desktop](./docs/images/landscape.gif) |
|------------------------------------------------------|-------------------------------------------------------|


## UI/UX and App Design

Heron uses [Material design and motion](https://m3.material.io/) and is heavily inspired by the
[Crane](https://m2.material.io/design/material-studies/crane.html) material study.

Libraries:

1. Geometric shapes are created with the [Jetpack shapes](https://developer.android.com/jetpack/androidx/releases/graphics) graphics library.
2. UX patterns like pinch to zoom, drag to dismiss, collapsing headers, multipane layouts, and
  and so on are implemented with the [Composables](https://github.com/tunjid/composables) library.

## Architecture 

This is a multi-module [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) project targeting Android, iOS and Desktop that follows the
[Android architecture guide](https://developer.android.com/topic/architecture).

For more details about this kind of architecture, take a look at the [Now in Android sample](https://github.com/android/nowinandroid) repository.
This app follows the same architecture principles it does, and the architecture decisions are very similar.

There are 6 kinds of modules:

1. `data-*` is the [data layer](https://developer.android.com/topic/architecture/data-layer) of the
  app containing models data and repository implementations for reading and writing that data.
  Data reads should never error, while writes are queued with a [WriteQueue]. Given the highly
  relational nature of the app, a class called a [MultipleEntitySaver] is used to save bluesky
  network models.
    - [Jetpack Room](https://developer.android.com/jetpack/androidx/releases/room)
      is used for persisting data with SQLite.
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

Dependency injection is implemented with the [Kotlin Inject](https://github.com/evant/kotlin-inject) library which constructs the dependency graph at build time
and therefore is compile time safe. Assisted injection is used for feature screens to pass navigation arguments information to the feature.

### Navigation

Navigation uses the [treenav experiment](https://github.com/tunjid/treeNav) to implement Android [adaptive navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive).
Specifically it uses a `ThreePane` configuration, where up to 3 navigation panes may be shown, with one reserved for back previews and another for modals.

### State production 

* State production follows the [Android guide to UI State Production](https://developer.android.com/topic/architecture/ui-layer/state-production).
* Each feature uses a single [Jetpack ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) as the [business logic state holder](https://developer.android.com/topic/architecture/ui-layer/stateholders).
* State is produced in a lifecycle aware way using the [Jetpack Lifecyle](https://developer.android.com/jetpack/androidx/releases/lifecycle) APIs.
  * The `CoroutineScope` for each `ViewModel` is obtained from the composition's `LocalLifecycleOwner`
* The specifics of producing state over time is implemented with the [Mutator library](https://github.com/tunjid/Mutator).
  * Inputs to the state production pipeline are passed to the mutator in the `inputs` argument, or derived from an action in `actionTransform`.
  * Every coroutine launched is limited to running when the lifecycle of the component displaying it is resumed. When the lifecyle
    is paused, the coroutines are cancelled after 2 seconds: `SharingStarted.WhileSubscribed(FeatureWhileSubscribed)`.
  * Each user `Action` is in a sealed hierarchy, and action parallelism is defined by the `Action.key`. Actions with different keys run in parallel
    while those in the same key are processed sequentially. Each distinct subtype of an `Action` hierarchy typically has it's own
    key unless sequential processing is required for example:
    * All subtypes of `Action.Navigation` typically share the same key.
    * All subtypes of pagination actions, also share the same key and are processed with the [Tiling library](https://github.com/tunjid/Tiler).
