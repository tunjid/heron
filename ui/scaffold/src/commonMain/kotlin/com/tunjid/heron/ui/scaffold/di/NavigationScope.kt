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

package com.tunjid.heron.ui.scaffold.di

/**
 * Dependency-graph scope for the app's navigation graph.
 *
 * Kept distinct from AppScope so that the route-matcher binding
 * containers contributed via `@ContributesTo(NavigationScope::class)` are aggregated only
 * into the lightweight navigation graph, while the full application bindings contributed via
 * `@ContributesTo(AppScope::class)` are aggregated only into the main app graph. This lets the
 * navigation graph be built first (to collect route matchers) without pulling in the entire
 * application object graph.
 */
abstract class NavigationScope private constructor()
