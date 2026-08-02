import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let appState: AppState

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(appState: appState)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    let appState: AppState

    var body: some View {
        ComposeView(appState: appState)
            .ignoresSafeArea(edges: .all)
            .ignoresSafeArea(.keyboard)
    }
}
