import Foundation
import SwiftUI
import ComposeApp

final class ImmersionModel: ObservableObject, ImmersionController {

    @Published private(set) var isImmersive: Bool = false

    /// Called from Kotlin on the main thread whenever the app enters or leaves immersion.
    func setImmersive(isImmersive: Bool) {
        guard self.isImmersive != isImmersive else { return }
        withAnimation {
            self.isImmersive = isImmersive
        }
    }
}
