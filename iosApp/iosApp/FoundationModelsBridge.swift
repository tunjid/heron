import ComposeApp
import Foundation
import FoundationModels

/// Swift implementation of the Kotlin `FoundationModelsBridge` protocol (declared in `:data:ml`,
/// exported via the `ComposeApp` framework). Kotlin/Native can't call the Swift-only
/// `FoundationModels` framework directly, so on-device inference lives here; `FoundationModelsEngine`
/// and `FoundationModelsManager` on the Kotlin side adapt these callbacks into Flows.
///
/// Every Foundation Models call is behind `#available(iOS 26)`; on older systems the bridge reports
/// the model as unavailable. `session` is stored as `Any?` because `LanguageModelSession` is iOS 26+
/// while this type is built against the app's iOS 17.4 deployment target.
final class AppleFoundationModelsBridge: FoundationModelsBridge {

    private var session: Any?
    private var currentTask: Task<Void, Never>?
    private let lock = NSRecursiveLock()
    private var availabilityListener: ((FoundationModelsAvailability) -> Void)?

    func setAvailabilityListener(listener: @escaping (FoundationModelsAvailability) -> Void) {
        availabilityListener = listener
    }

    func refreshAvailability() {
        availabilityListener?(currentAvailability())
    }

    private func currentAvailability() -> FoundationModelsAvailability {
        guard #available(iOS 26.0, *) else {
            return .devicenoteligible
        }
        switch SystemLanguageModel.default.availability {
        case .available:
            return .available
        case .unavailable(let reason):
            switch reason {
            case .deviceNotEligible:
                return .devicenoteligible
            case .appleIntelligenceNotEnabled:
                return .appleintelligencenotenabled
            case .modelNotReady:
                return .modelnotready
            @unknown default:
                return .devicenoteligible
            }
        }
    }

    func load(onReady: @escaping () -> Void, onError: @escaping (String) -> Void) {
        guard #available(iOS 26.0, *) else {
            onError("On-device inference requires iOS 26.")
            return
        }
        guard case .available = SystemLanguageModel.default.availability else {
            onError("The on-device model is not available.")
            return
        }
        lock.lock()
        session = LanguageModelSession()
        lock.unlock()
        onReady()
    }

    func generate(
        prompt: String,
        temperature: Float,
        topK: Int32,
        topP: Float,
        onToken: @escaping (String) -> Void,
        onComplete: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard #available(iOS 26.0, *) else {
            onError("On-device inference requires iOS 26.")
            return
        }
        lock.lock()
        currentTask?.cancel()
        currentTask = nil
        let activeSession = (session as? LanguageModelSession) ?? LanguageModelSession()
        session = activeSession
        lock.unlock()

        // Foundation Models exposes either top-k or top-p sampling, not both; prefer top-p when set.
        let sampling: GenerationOptions.SamplingMode = topP < 1.0
            ? .random(probabilityThreshold: Double(topP))
            : .random(top: Int(topK))
        let options = GenerationOptions(
            sampling: sampling,
            temperature: Double(temperature)
        )

        let task = Task {
            do {
                // Snapshots are cumulative; emit only the newly-appended text as a delta.
                var previous = ""
                for try await snapshot in activeSession.streamResponse(to: prompt, options: options) {
                    if Task.isCancelled { break }
                    let content = snapshot.content
                    let delta = String(content.dropFirst(previous.count))
                    previous = content
                    if !delta.isEmpty { onToken(delta) }
                }
                if !Task.isCancelled { onComplete() }
            } catch is CancellationError {
                // Cancelled generation is not an error.
            } catch {
                if !Task.isCancelled { onError(error.localizedDescription) }
            }
        }
        lock.lock()
        currentTask = task
        lock.unlock()
    }

    func cancel() {
        lock.lock()
        defer { lock.unlock() }
        currentTask?.cancel()
        currentTask = nil
    }

    func reset() {
        lock.lock()
        defer { lock.unlock() }
        currentTask?.cancel()
        currentTask = nil
        session = nil
    }
}
