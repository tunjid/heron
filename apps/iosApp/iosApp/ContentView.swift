import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let appState: AppState
    let immersionController: ImmersionController

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            appState: appState,
            immersionController: immersionController
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    let appState: AppState
    @ObservedObject var immersion: ImmersionModel

    var body: some View {
        ComposeView(appState: appState, immersionController: immersion)
            .ignoresSafeArea(edges: .all)
            .ignoresSafeArea(.keyboard)
            .statusBarHidden(immersion.isImmersive)
            .persistentSystemOverlays(immersion.isImmersive ? .hidden : .automatic)
    }
}
