import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let appState: AppState
    let immersionController: ImmersionController

    func makeUIViewController(context: Context) -> UIViewController {
        let host = VerticalBarHostingController()
        host.setContent(
            MainViewControllerKt.MainViewController(
                appState: appState,
                immersionController: immersionController,
                verticalBarController: host
            )
        )
        return host
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

final class VerticalBarHostingController: UIViewController, VerticalBarController {

    private var listener: VerticalBarListener?
    private var lastReport: VerticalBarReport?

    func setContent(_ content: UIViewController) {
        addChild(content)
        content.view.frame = view.bounds
        content.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(content.view)
        content.didMove(toParent: self)
    }

    func setListener(listener: VerticalBarListener?) {
        self.listener = listener
        lastReport = nil
        view.setNeedsLayout()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        if #available(iOS 27.1, *) {
            registerForTraitChanges(
                UITraitCollection.systemTraitsAffectingVerticalBarEdge
            ) { (controller: VerticalBarHostingController, _: UITraitCollection) in
                controller.view.setNeedsLayout()
            }
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        reportVerticalBar()
    }

    private func reportVerticalBar() {
        guard let listener else { return }
        let report = currentReport()
        guard report != lastReport else { return }
        lastReport = report
        listener.onVerticalBarChanged(
            edge: report.isOnLeft.map { $0 ? NavRailEdge.start : NavRailEdge.end },
            width: report.width,
            centerFromEdge: report.centerFromEdge
        )
    }

    private func currentReport() -> VerticalBarReport {
        guard #available(iOS 27.1, *) else { return .absent }
        let isRightToLeft = view.effectiveUserInterfaceLayoutDirection == .rightToLeft
        let edge: UIRectEdge
        switch traitCollection.verticalBarEdge {
        case .leading:
            edge = isRightToLeft ? .right : .left
        case .trailing:
            edge = isRightToLeft ? .left : .right
        default:
            return .absent
        }
        let region = view.layoutGuide(
            for: .bar(onEdge: edge, extent: verticalBarExtent)
        ).layoutFrame
        let isOnLeft = edge == .left
        return VerticalBarReport(
            isOnLeft: isOnLeft,
            width: Double(isOnLeft ? view.safeAreaInsets.left : view.safeAreaInsets.right),
            centerFromEdge: Double(isOnLeft ? region.midX - view.bounds.minX : view.bounds.maxX - region.midX)
        )
    }
}

private struct VerticalBarReport: Equatable {
    let isOnLeft: Bool?
    let width: Double
    let centerFromEdge: Double

    static let absent = VerticalBarReport(isOnLeft: nil, width: 0, centerFromEdge: 0)
}

/// The thickness of the glass navigation rail, matching the bottom tab bar's capsule.
private let verticalBarExtent: CGFloat = 62

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
