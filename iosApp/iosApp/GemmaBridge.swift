import ComposeApp
import Foundation
import LiteRTLM

/// Swift implementation of the Kotlin `IosInferenceBridge` protocol (declared in
/// `:data:ml`, exported via the `ComposeApp` framework). Kotlin/Native can't call
/// the Swift-only `LiteRTLM` package directly, so on-device inference lives here and
/// `IosInferenceEngine` on the Kotlin side adapts these callbacks back into a Flow.
///
/// Passed to `EntryPoint_iosKt.createAppState(inferenceBridge:)` at startup.
final class GemmaBridge: IosInferenceBridge {

    private var engine: Engine?
    private var currentTask: Task<Void, Never>?

    func load(
        modelPath: String,
        maxTokens: Int32,
        onReady: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        Task {
            do {
                let config = try EngineConfig(
                    modelPath: modelPath,
                    backend: .cpu(),
                    maxNumTokens: Int(maxTokens),
                    cacheDir: NSTemporaryDirectory()
                )
                let engine = Engine(engineConfig: config)
                try await engine.initialize()
                self.engine = engine
                onReady()
            } catch {
                onError(error.localizedDescription)
            }
        }
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
        guard let engine = engine else {
            onError("generate() called before load()")
            return
        }
        currentTask = Task {
            do {
                let samplerConfig = try SamplerConfig(
                    topK: Int(topK),
                    topP: Double(topP),
                    temperature: Double(temperature)
                )
                let conversation = try await engine.createConversation(
                    with: ConversationConfig(samplerConfig: samplerConfig)
                )
                for try await chunk in conversation.sendMessageStream(Message(prompt)) {
                    if Task.isCancelled { break }
                    onToken(chunk.toString)
                }
                onComplete()
            } catch {
                onError(error.localizedDescription)
            }
        }
    }

    func cancel() {
        currentTask?.cancel()
        currentTask = nil
    }

    func close() {
        cancel()
        engine = nil
    }
}
