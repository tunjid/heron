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

package com.tunjid.heron

/**
 * Swift implemented sink for the app's immersion state, exported via the `ComposeApp` framework.
 *
 * The app is hosted in SwiftUI, and `UIHostingController` derives its status bar preferences from
 * the SwiftUI environment rather than from view controllers vended by a
 * `UIViewControllerRepresentable`. Overriding `prefersStatusBarHidden` on the Compose
 * `UIViewController` is therefore ignored, and there is no UIKit hook at all for the home
 * indicator. Both live behind SwiftUI's `statusBarHidden` and `persistentSystemOverlays`
 * modifiers, so Kotlin pushes the state out to Swift and lets SwiftUI apply it.
 *
 * Declared as an interface rather than a lambda so the parameter exports as a Swift `Bool`;
 * `Boolean` inside a Kotlin function type crosses over as `KotlinBoolean`.
 *
 * Always invoked on the main thread.
 */
interface ImmersionController {
    fun setImmersive(isImmersive: Boolean)
}
