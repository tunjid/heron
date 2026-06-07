<!--
    Copyright 2024 Adetunji Dahunsi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

# AGENTS.md

Operational guide for coding agents working in Heron. The [README](README.md) is the deep,
human-facing reference for architecture and build/publishing; this file is the terse map of *where
things live, the pattern to copy, the commands to run, and the rules to respect*. When something here
needs more depth, it links into the README rather than restating it.

## What this is

Heron is an adaptive, reactive, offline-first [Bluesky](https://bsky.app)/ATProto client built with
Kotlin Multiplatform and Jetpack Compose. It targets **Android, iOS, and Desktop (JVM)** — there is
**no web/wasm target**. See [README · Architecture](README.md#architecture) for the full picture.

## Module map

Five kinds of modules, in a layered dependency graph: `data → ui → scaffold → feature → composeApp`
(arrows point from a layer to what's built on top of it; compile-time deps run the opposite way).
The DI graph mirrors these layers — see [README · Dependency Injection](README.md#dependency-injection).

| Kind | Modules | Role |
|---|---|---|
| `data:*` | `data/core`, `data/models`, `data/database`, `data/lexicons` (generated), … | Root layer. Repositories, Room, Ktor/[Ozone](https://github.com/christiandeange/ozone) ATProto, `WriteQueue`. Depends on no other layer. Reads never error; writes are queued. |
| `ui:*` | `ui/core`, `ui/media`, `ui/timeline`, `ui/tiling`, `ui/profile` | Reusable Compose components/effects. `ui/timeline` holds `TimelineState`/`timelineStateHolder` (paging via [Tiler](https://github.com/tunjid/Tiler)). |
| `scaffold` | `scaffold` (single module) | `AppState`, navigation, pane coordination, back previews. Exposes `PaneScaffoldState` to features. |
| `feature:*` | ~23 destinations: `feature/feed`, `feature/profile`, `feature/home`, … | Navigation destinations (screens). `feature/template` is the shared abstraction they depend on — **not** a copy-me scaffold. |
| app modules | `composeApp`, `androidApp`, `desktopApp` | `composeApp` = shared assembly + per-platform `EntryPoint*.kt`. `androidApp`/`desktopApp` are OS launchers; iOS launches from the `iosApp` Xcode project. |

## Anatomy of a feature

Every `feature:*` module has the same four parts. `feature/feed` is the reference example
(`feature/feed/src/commonMain/kotlin/com/tunjid/heron/feed/`):

- **`State.kt`** — an `interface State` with an immutable `@Snapshottable`/`@Serializable`
  implementation, plus a sealed `Action(val key: String)` hierarchy.
- **`<Name>ViewModel.kt`** — an `@AssistedInject` ViewModel (assisted args carry the navigation
  route) that delegates to `scope.actionSuspendingStateMutator(...)`. Mutation handlers live in the
  `producer` lambda; state is produced with
  `started = SharingStarted.WhileSubscribed(FeatureWhileSubscribed)`.
- **`<Name>Screen.kt`** — a composable taking `state`, `actions: (Action) -> Unit`, and
  `PaneScaffoldState`.
- **`di/Bindings.kt`** — a `@BindingContainer` exposing two binding sets:
  - `<Name>Bindings` contributing the `PaneEntry` screen factory (`@IntoMap`) into the app's
    `entryMap`, and
  - `<Name>NavigationBindings` contributing a `RouteMatcher` (`@IntoMap`, via `urlRouteMatcher`)
    into the `routeMatcherMap`.

See [README · State production](README.md#state-production) for the Action-key parallelism model and
the Mutator pipeline.

## Recipe: add a new feature

Metro builds and verifies the DI graph at **compile time**, so a missed wiring point is a build
error with a clear message — *building the feature is the test*. The four places to touch:

1. **Create `feature/<name>/`** with a `build.gradle.kts` applying `id("kotlin-library-convention")`
   plus the Compose and serialization plugins (copy `feature/template/build.gradle.kts`), and the
   four source files above.
2. **Register the module** in [`settings.gradle.kts`](settings.gradle.kts) — add `":feature:<name>"`
   to the `include(...)` block (alongside `":feature:feed"`).
3. **Add the bindings to the graph**: `@Includes <name>Bindings` in
   [`AppGraph`](composeApp/src/commonMain/kotlin/com/tunjid/heron/di/AppGraph.kt) and
   `@Includes <name>NavigationBindings` in
   [`AppNavigationGraph`](composeApp/src/commonMain/kotlin/com/tunjid/heron/di/AppNavigationGraph.kt).
4. **Instantiate both** in the graph-factory call in
   [`EntryPoint.kt`](composeApp/src/commonMain/kotlin/com/tunjid/heron/EntryPoint.kt) (the
   `<name>Bindings = <Name>Bindings(...)` list).

## Build, test & format

Use the Gradle wrapper (`./gradlew`). Dependencies are versioned in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml) — add via the version catalog, never
hardcoded coordinates.

Formatting rules live in [`.editorconfig`](.editorconfig) (4-space Kotlin indent, ktlint
`intellij_idea` style, trailing commas allowed). `spotlessApply` runs ktlint against these settings,
so honour them — don't reformat against a different convention.

| Task | Command |
|---|---|
| Format (run before committing) | `./gradlew spotlessApply` — ktlint via Spotless, applied to all modules |
| Format check only | `./gradlew spotlessCheck` |
| Data-layer tests | `./gradlew testDataLayer` — aggregates `allTests` for every `:data:*` module |
| Single module's tests | `./gradlew :feature:feed:allTests` (or `:data:core:testDebugUnitTest`) |
| Android debug build | `./gradlew :androidApp:assembleDebug` (or `:androidApp:installDebug`) |
| Desktop run | `./gradlew :desktopApp:run` |
| Desktop package (macOS) | `./gradlew packageReleaseDmg` |

iOS is built from the `iosApp` Xcode project against the `composeApp` framework — not a pure Gradle
run. See [README · Building](README.md#building).

## Conventions & gotchas

- **State is immutable + `@Snapshottable`.** Mutate only through the mutator's `producer`; never hold
  mutable state in a composable or ViewModel field.
- **`Action.key` controls parallelism.** Same key → processed sequentially; different keys → run in
  parallel. Navigation actions share a key; pagination actions share a key (Tiler).
- **Reads never error; all writes go through the `WriteQueue`.** Enqueue via
  `writeQueue.enqueue(Writable.*)` (offline-first, persisted, retried) — don't call the network
  directly from a feature.
- **Lifecycle-aware collection** uses `SharingStarted.WhileSubscribed(FeatureWhileSubscribed)`;
  coroutines stop ~2s after the displaying component pauses.
- **New `.kt` files need the Apache 2.0 licence header** (Spotless/ktlint enforces it — copy it from
  any existing file).
- **One parameter per line** for any function/constructor that takes arguments — declarations and
  call sites both. The `.editorconfig` already forces this for signatures
  (`force_multiline_when_parameter_count_greater_or_equal_than = 1`); match it at call sites too.
- **Use named arguments** when invoking a method that accepts them — favour readability over
  positional brevity.
- **Keep shared code in `commonMain`;** only put platform-specific code in `androidMain`/`iosMain`/
  `desktopMain`.

## Don't hand-edit / generated

- **`data/lexicons`** ATProto bindings are generated by the Ozone lexicon plugin
  (`ozoneLexiconGenerator`) — regenerate, don't edit by hand.
- **iOS signing & `iosApp.xcodeproj/project.pbxproj` patching**, `aps-environment` flipping, and the
  Kotlin/Native devirtualization compiler flag are deliberate CI/build concerns documented in
  [README · iOS publishing notes](README.md#ios-publishing-notes) — don't "fix" them casually.
- **`**/build/**`** output and `.hprof` heap dumps are not source.

## Pointers

- Deep dives: [Architecture](README.md#architecture) · [Dependency Injection](README.md#dependency-injection)
  · [Navigation](README.md#navigation) · [State production](README.md#state-production)
  · [Building](README.md#building)
- Upstream libraries that shape the code: [Metro](https://github.com/ZacSweers/metro) (DI) ·
  [Mutator](https://github.com/tunjid/Mutator) (state) · [Tiler](https://github.com/tunjid/Tiler)
  (paging) · [treeNav](https://github.com/tunjid/treeNav) (navigation) ·
  [Ozone](https://github.com/christiandeange/ozone) (ATProto) ·
  [Composables](https://github.com/tunjid/composables) (UX).
