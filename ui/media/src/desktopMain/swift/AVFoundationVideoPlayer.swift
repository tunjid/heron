// Originally from ComposeMediaPlayer by Elie G.
// https://github.com/kdroidFilter/ComposeMediaPlayer
//
// MIT License
//
// Copyright (c) 2025 Elie G.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

import AVFoundation
import CoreGraphics
import CoreVideo
import Foundation
import AppKit

/// Class that manages video playback and frame capture into an optimized shared buffer.
/// Frame capture rate adapts to the lower of screen refresh rate and video frame rate.
/// Includes full HLS (HTTP Live Streaming) support with adaptive bitrate streaming.
class SharedVideoPlayer {
    private var player: AVPlayer?
    private var videoOutput: AVPlayerItemVideoOutput?

    // Timer for capturing frames at adaptive rate
    private var displayLink: Timer?

    // Track the video's native frame rate
    private var videoFrameRate: Float = 0.0

    // Track the screen's refresh rate
    private var screenRefreshRate: Float = 60.0

    // The actual capture frame rate (minimum of video and screen rates)
    private var captureFrameRate: Float = 0.0

    // Shared buffer to store the frame in BGRA format (no conversion needed)
    private var frameBuffer: UnsafeMutablePointer<UInt32>?
    private var bufferCapacity: Int = 0

    // Frame dimensions
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    // Audio volume control (0.0 to 1.0)
    private var volume: Float = 1.0

    // Flag to track if playback is active
    private var isPlaying: Bool = false
    private var isReadyForPlayback = false
    private var pendingPlay = false

    // HLS-specific properties
    private var isHLSStream: Bool = false
    private var availableBitrates: [Float] = []
    private var currentBitrate: Float = 0
    private var preferredPeakBitRate: Double = 0
    private var bufferStatus: Float = 0.0
    private var isBuffering: Bool = false
    private var networkStatus: String = "Unknown"

    // Observers for HLS monitoring
    private var playerItemObserver: NSKeyValueObservation?
    private var playerObserver: NSKeyValueObservation?
    private var timeControlStatusObserver: NSKeyValueObservation?
    private var bufferEmptyObserver: NSKeyValueObservation?
    private var bufferLikelyToKeepUpObserver: NSKeyValueObservation?
    private var bufferFullObserver: NSKeyValueObservation?

    // HLS Error tracking
    private var lastError: String? = nil
    private var errorCount: Int = 0

    // Status code constants
    private let STATUS_CODE_PAUSED: Int32 = 0
    private let STATUS_CODE_PLAYING: Int32 = 1
    private let STATUS_CODE_BUFFERING: Int32 = 2

    // JNA callback types and storage
    private var statusCallback: (@convention(c) (UnsafeRawPointer?, Int32) -> Void)?
    private var timeCallback: (@convention(c) (UnsafeRawPointer?, Double, Double) -> Void)?
    private var frameCallback: (@convention(c) (UnsafeRawPointer?) -> Void)?
    private var endOfPlaybackCallback: (@convention(c) (UnsafeRawPointer?) -> Void)?
    private var callbackContext: UnsafeRawPointer?
    private var periodicTimeObserver: Any?
    private var endOfPlaybackObserver: NSObjectProtocol?
    private var timeObserverInterval: Double = 0.25

    init() {
        // Detect screen refresh rate
        detectScreenRefreshRate()

        // Configure AVAudioSession for better HLS audio handling
        configureAudioSession()
    }

    /// Configures the audio session for optimal HLS playback
    private func configureAudioSession() {
        // Note: AVAudioSession is iOS/tvOS only. For macOS, we'll use different audio configuration
        // macOS handles audio differently through Core Audio
    }

    /// Detects the current screen refresh rate
    private func detectScreenRefreshRate() {
        if let mainScreen = NSScreen.main {
            // Use CoreVideo DisplayLink to get refresh rate on macOS
            var displayID: CGDirectDisplayID = CGMainDisplayID()
            if let screenNumber = mainScreen.deviceDescription[
                NSDeviceDescriptionKey("NSScreenNumber")] as? NSNumber
            {
                displayID = CGDirectDisplayID(screenNumber.uint32Value)
            }

            var displayLink: CVDisplayLink?
            let error = CVDisplayLinkCreateWithCGDisplay(displayID, &displayLink)

            if error == kCVReturnSuccess, let link = displayLink {
                let period = CVDisplayLinkGetNominalOutputVideoRefreshPeriod(link)
                let timeValue = period.timeValue
                let timeScale = period.timeScale

                if timeValue > 0 && timeScale > 0 {
                    // Convert to Hz (frames per second)
                    let refreshRate = Double(timeScale) / Double(timeValue)
                    screenRefreshRate = Float(refreshRate)
                }
            } else {
                // Fallback if we can't get the refresh rate
                screenRefreshRate = 60.0
            }
        } else {
            screenRefreshRate = 60.0
        }
    }

    /// Checks if the URL is an HLS stream
    private func isHLSUrl(_ url: URL) -> Bool {
        let urlString = url.absoluteString.lowercased()
        return urlString.contains(".m3u8") ||
            urlString.contains("/playlist.m3u8") ||
            urlString.contains("/master.m3u8") ||
            urlString.contains("format=m3u8")
    }

    /// Configures the asset for HLS streaming
    private func configureHLSAsset(_ asset: AVURLAsset) -> AVURLAsset {
        // Configure asset for optimal HLS streaming
        let options: [String: Any] = [
            AVURLAssetPreferPreciseDurationAndTimingKey: true
        ]

        // Create new asset with HLS-optimized options
        return AVURLAsset(url: asset.url, options: options)
    }

    /// Sets up HLS-specific monitoring
    private func setupHLSMonitoring(for item: AVPlayerItem) {
        // Monitor playback buffer
        bufferEmptyObserver = item.observe(\.isPlaybackBufferEmpty, options: [.new]) { [weak self] item, _ in
            self?.handleBufferEmpty(item.isPlaybackBufferEmpty)
        }

        bufferLikelyToKeepUpObserver = item.observe(\.isPlaybackLikelyToKeepUp, options: [.new]) { [weak self] item, _ in
            self?.handleBufferLikelyToKeepUp(item.isPlaybackLikelyToKeepUp)
        }

        bufferFullObserver = item.observe(\.isPlaybackBufferFull, options: [.new]) { [weak self] item, _ in
            self?.handleBufferFull(item.isPlaybackBufferFull)
        }

        // Monitor loaded time ranges for buffer status
        playerItemObserver = item.observe(\.loadedTimeRanges, options: [.new]) { [weak self] item, _ in
            self?.updateBufferStatus(from: item)
        }

        // Monitor access log for bitrate changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleAccessLog(_:)),
            name: .AVPlayerItemNewAccessLogEntry,
            object: item
        )

        // Monitor error log
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleErrorLog(_:)),
            name: .AVPlayerItemNewErrorLogEntry,
            object: item
        )

        // Monitor playback stalls
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handlePlaybackStall(_:)),
            name: .AVPlayerItemPlaybackStalled,
            object: item
        )
    }

    /// Handles buffer empty state
    private func handleBufferEmpty(_ isEmpty: Bool) {
        if isEmpty {
            isBuffering = true
            print("HLS: Buffer empty, buffering...")
        }
    }

    /// Handles buffer likely to keep up state
    private func handleBufferLikelyToKeepUp(_ isLikely: Bool) {
        if isLikely {
            isBuffering = false
            print("HLS: Buffer recovered, playback can continue")
        }
    }

    /// Handles buffer full state
    private func handleBufferFull(_ isFull: Bool) {
        if isFull {
            print("HLS: Buffer is full")
        }
    }

    /// Handles time control status changes
    private func handleTimeControlStatus(_ status: AVPlayer.TimeControlStatus) {
        switch status {
        case .paused:
            networkStatus = "Paused"
            statusCallback?(callbackContext, STATUS_CODE_PAUSED)
        case .waitingToPlayAtSpecifiedRate:
            networkStatus = "Buffering"
            isBuffering = true
            statusCallback?(callbackContext, STATUS_CODE_BUFFERING)
        case .playing:
            networkStatus = "Playing"
            isBuffering = false
            statusCallback?(callbackContext, STATUS_CODE_PLAYING)
        @unknown default:
            networkStatus = "Unknown"
        }
    }

    /// Updates buffer status from loaded time ranges
    private func updateBufferStatus(from item: AVPlayerItem) {
        guard let timeRange = item.loadedTimeRanges.first?.timeRangeValue else {
            bufferStatus = 0.0
            return
        }

        let startSeconds = CMTimeGetSeconds(timeRange.start)
        let durationSeconds = CMTimeGetSeconds(timeRange.duration)
        let currentSeconds = CMTimeGetSeconds(item.currentTime())

        if currentSeconds > 0 {
            let bufferedSeconds = startSeconds + durationSeconds - currentSeconds
            // Normalize to 0-1 range (assuming 10 seconds is "full" buffer)
            bufferStatus = Float(min(bufferedSeconds / 10.0, 1.0))
        }
    }

    /// Handles access log entries for HLS monitoring
    @objc private func handleAccessLog(_ notification: Notification) {
        guard let item = notification.object as? AVPlayerItem,
              let accessLog = item.accessLog(),
              let lastEvent = accessLog.events.last else { return }

        // Update current bitrate
        if lastEvent.indicatedBitrate > 0 {
            currentBitrate = Float(lastEvent.indicatedBitrate)
        }

        // Log HLS streaming statistics
        print("""
                  HLS Access Log:
                  - Indicated Bitrate: \(lastEvent.indicatedBitrate) bps
                  - Observed Bitrate: \(lastEvent.observedBitrate) bps
                  - Stall Count: \(lastEvent.numberOfStalls)
                  - Downloaded Bytes: \(lastEvent.numberOfBytesTransferred)
                  - Segments Downloaded: \(lastEvent.numberOfMediaRequests)
              """)
    }

    /// Handles error log entries
    @objc private func handleErrorLog(_ notification: Notification) {
        guard let item = notification.object as? AVPlayerItem,
              let errorLog = item.errorLog(),
              let lastEvent = errorLog.events.last else { return }

        errorCount += 1
        lastError = lastEvent.errorComment ?? "Unknown HLS error"

        print("""
                  HLS Error Log:
                  - Error Domain: \(lastEvent.errorDomain)
                  - Error Code: \(lastEvent.errorStatusCode)
                  - Error Comment: \(lastEvent.errorComment ?? "None")
                  - Server Address: \(lastEvent.serverAddress ?? "Unknown")
              """)
    }

    /// Handles playback stalls
    @objc private func handlePlaybackStall(_ notification: Notification) {
        print("HLS: Playback stalled")
        isBuffering = true
    }

    /// Extracts available bitrates from HLS variants
    private func extractHLSVariants(from asset: AVAsset) {
        if #available(macOS 13.0, *) {
            Task {
                do {
                    // For HLS streams, try to get variant information
                    if let urlAsset = asset as? AVURLAsset {
                        let variants = try await urlAsset.load(.variants)

                        availableBitrates = []
                        for variant in variants {
                            if let peakBitRate = variant.peakBitRate {
                                availableBitrates.append(Float(peakBitRate))
                            }
                        }

                        if !availableBitrates.isEmpty {
                            availableBitrates.sort()
                            print("HLS: Available bitrates: \(availableBitrates)")
                        }
                    }
                } catch {
                    print("Error loading HLS variants: \(error.localizedDescription)")
                }
            }
        }
    }

    /// Detects the video's native frame rate from its asset
    private func detectVideoFrameRate(from asset: AVAsset) {
        // For HLS streams, default to 30 fps as it's variable
        if isHLSStream {
            videoFrameRate = 30.0
            updateCaptureFrameRate()
            return
        }

        asset.loadTracks(withMediaType: .video) { [self] tracks, error in
            guard let videoTrack = tracks?.first, error == nil else {
                print(
                    "Erreur lors du chargement des pistes vidéo : \(error?.localizedDescription ?? "Inconnue")"
                )
                return
            }

            // Replace deprecated nominalFrameRate property
            if #available(macOS 13.0, *) {
                Task {
                    do {
                        let frameRate = try await videoTrack.load(.nominalFrameRate)
                        self.videoFrameRate = Float(frameRate)
                        if self.videoFrameRate <= 0 {
                            // Fallback to common default if detection fails
                            self.videoFrameRate = 30.0
                        }

                        // Set capture rate to the lower of the two rates
                        self.updateCaptureFrameRate()
                    } catch {
                        print("Error loading nominal frame rate: \(error.localizedDescription)")
                        // Fallback to common default if detection fails
                        self.videoFrameRate = 30.0
                        self.updateCaptureFrameRate()
                    }
                }
            } else {
                // Use deprecated property for older OS versions
                videoFrameRate = Float(videoTrack.nominalFrameRate)
                if videoFrameRate <= 0 {
                    // Fallback to common default if detection fails
                    videoFrameRate = 30.0
                }

                // Set capture rate to the lower of the two rates
                updateCaptureFrameRate()
            }
        }
    }

    /// Updates the capture frame rate based on screen and video rates
    private func updateCaptureFrameRate() {
        captureFrameRate = min(screenRefreshRate, videoFrameRate)
        // Update display link if it exists
        if isPlaying {
            configureDisplayLink()
        }
    }

    /// Opens the video from the given URI (local or network)
    func openUri(_ uri: String) {
        isReadyForPlayback = false
        pendingPlay = false

        // Clean up previous observers
        cleanupObservers()

        // Determine the URL (local or network)
        let url: URL = {
            if let parsedURL = URL(string: uri), parsedURL.scheme != nil {
                return parsedURL
            } else {
                return URL(fileURLWithPath: uri)
            }
        }()

        // Check if this is an HLS stream
        isHLSStream = isHLSUrl(url)

        if isHLSStream {
            print("Detected HLS stream: \(url)")
        }

        var asset = AVURLAsset(url: url)
        // Configure asset for HLS if needed
        if isHLSStream {
            asset = configureHLSAsset(asset)
        }

        // Detect video frame rate
        detectVideoFrameRate(from: asset)

        // Retrieve the video track to obtain the actual dimensions
        asset.loadTracks(withMediaType: .video) { [self] tracks, error in
            guard let videoTrack = tracks?.first, error == nil else {
                print(
                    "Erreur lors du chargement des pistes vidéo : \(error?.localizedDescription ?? "Inconnue")"
                )
                // For HLS streams without video track info yet, use default dimensions
                if isHLSStream {
                    frameWidth = 1920
                    frameHeight = 1080
                    setupFrameBuffer()
                    setupVideoOutputAndPlayer(with: asset)
                }
                return
            }

            if #available(macOS 13.0, *) {
                Task { [weak self, asset] in
                    guard let self = self else { return }
                    do {
                        // Use the modern API to load naturalSize and preferredTransform
                        let naturalSize = try await videoTrack.load(.naturalSize)
                        let transform = try await videoTrack.load(.preferredTransform)

                        let effectiveSize = naturalSize.applying(transform)
                        self.frameWidth = Int(abs(effectiveSize.width))
                        self.frameHeight = Int(abs(effectiveSize.height))

                        // Continue with buffer allocation and setup
                        self.setupFrameBuffer()
                        self.setupVideoOutputAndPlayer(with: asset)
                    } catch {
                        print("Error loading video track properties: \(error.localizedDescription)")
                        // Use default dimensions for HLS if loading fails
                        if self.isHLSStream {
                            self.frameWidth = 1920
                            self.frameHeight = 1080
                            self.setupFrameBuffer()
                            self.setupVideoOutputAndPlayer(with: asset)
                        }
                    }
                }
            } else {
                // Fallback for older OS versions using deprecated properties
                let naturalSize = videoTrack.naturalSize
                let transform = videoTrack.preferredTransform

                let effectiveSize = naturalSize.applying(transform)
                frameWidth = Int(abs(effectiveSize.width))
                frameHeight = Int(abs(effectiveSize.height))

                // Continue with buffer allocation and setup
                setupFrameBuffer()
                setupVideoOutputAndPlayer(with: asset)
            }
        }
    }

    // Helper method to setup frame buffer
    private func setupFrameBuffer() {
        // Allocate or reuse the shared buffer if capacity matches
        let totalPixels = frameWidth * frameHeight
        if let buffer = frameBuffer, bufferCapacity == totalPixels {
            buffer.initialize(repeating: 0, count: totalPixels)
        } else {
            frameBuffer?.deallocate()
            frameBuffer = UnsafeMutablePointer<UInt32>.allocate(capacity: totalPixels)
            frameBuffer?.initialize(repeating: 0, count: totalPixels)
            bufferCapacity = totalPixels
        }
    }

    // Helper method to setup video output and player
    private func setupVideoOutputAndPlayer(with asset: AVAsset) {
        // Create attributes for the CVPixelBuffer (BGRA format) with IOSurface for better performance
        let pixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
            kCVPixelBufferWidthKey as String: frameWidth,
            kCVPixelBufferHeightKey as String: frameHeight,
            kCVPixelBufferIOSurfacePropertiesKey as String: [:],
        ]
        videoOutput = AVPlayerItemVideoOutput(pixelBufferAttributes: pixelBufferAttributes)

        let item = AVPlayerItem(asset: asset)

        // Configure for HLS if needed
        if isHLSStream {
            // Set buffer duration for HLS
            item.preferredForwardBufferDuration = 5.0  // 5 seconds of buffer

            // Set initial preferred peak bitrate if specified
            if preferredPeakBitRate > 0 {
                item.preferredPeakBitRate = preferredPeakBitRate
            }

            // Enable automatic waiting behavior for HLS
            if #available(macOS 13.0, *) {
                item.automaticallyPreservesTimeOffsetFromLive = true
            }

            // Setup HLS monitoring
            setupHLSMonitoring(for: item)
        }

        if let output = videoOutput {
            item.add(output)
        }

        player = AVPlayer(playerItem: item)

        // Attach observers for callbacks that were registered before the player existed
        attachCallbackObservers()

        // Configure player for HLS
        if isHLSStream {
            player?.automaticallyWaitsToMinimizeStalling = true
        }

        // Set initial volume
        player?.volume = volume

        // For non-HLS content, capture initial frame
        if !isHLSStream {
            captureInitialFrame()
        }

        // Mark as ready for playback
        self.isReadyForPlayback = true

        // If playback was pending, start playback
        if self.pendingPlay {
            DispatchQueue.main.async {
                self.play()
            }
        }
    }

    /// Captures initial frame to display without starting the display link
    private func captureInitialFrame() {
        guard let output = videoOutput, player?.currentItem != nil, !isHLSStream else { return }

        // Seek to the beginning to ensure we have a frame
        let zeroTime = CMTime.zero
        player?.seek(to: zeroTime)

        // Try to get the first frame
        if output.hasNewPixelBuffer(forItemTime: zeroTime),
           let pixelBuffer = output.copyPixelBuffer(forItemTime: zeroTime, itemTimeForDisplay: nil)
        {
            updateLatestFrameData(from: pixelBuffer)
        }
    }

    /// Configures the timer with the appropriate frame rate
    private func configureDisplayLink() {
        stopDisplayLink()  // Ensure previous link is invalidated

        // For macOS, use a timer with the appropriate interval
        let interval = 1.0 / Double(captureFrameRate)
        displayLink = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
            self?.captureFrame()
        }
    }

    /// Stops the timer
    private func stopDisplayLink() {
        displayLink?.invalidate()
        displayLink = nil
    }

    /// Captures the latest frame from the video output if available.
    @objc private func captureFrame() {
        guard let output = videoOutput,
              let item = player?.currentItem,
              isPlaying
        else { return }  // Skip capture if video is not playing

        let currentTime = item.currentTime()
        if output.hasNewPixelBuffer(forItemTime: currentTime),
           let pixelBuffer = output.copyPixelBuffer(
               forItemTime: currentTime, itemTimeForDisplay: nil)
        {
            updateLatestFrameData(from: pixelBuffer)
            frameCallback?(callbackContext)
        }
    }

    /// Directly copies the content of the pixelBuffer into the shared buffer without conversion.
    private func updateLatestFrameData(from pixelBuffer: CVPixelBuffer) {
        guard let destBuffer = frameBuffer else { return }

        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        guard let srcBaseAddress = CVPixelBufferGetBaseAddress(pixelBuffer) else { return }
        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)
        let srcBytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)

        // For HLS, dimensions might change dynamically
        if isHLSStream && (width != frameWidth || height != frameHeight) {
            print("HLS: Resolution changed from \(frameWidth)x\(frameHeight) to \(width)x\(height)")
            frameWidth = width
            frameHeight = height
            setupFrameBuffer()
            guard frameBuffer != nil else { return }
        }

        guard width == frameWidth, height == frameHeight else {
            print("Unexpected dimensions: \(width)x\(height)")
            return
        }

        if srcBytesPerRow == width * 4 {
            memcpy(destBuffer, srcBaseAddress, height * srcBytesPerRow)
        } else {
            for row in 0..<height {
                let srcRow = srcBaseAddress.advanced(by: row * srcBytesPerRow)
                let destRow = destBuffer.advanced(by: row * width)
                memcpy(destRow, srcRow, width * 4)
            }
        }
    }

    /// Starts video playback and begins frame capture at the optimized frame rate.
    func play() {
        if isReadyForPlayback {
            isPlaying = true
            player?.play()
            configureDisplayLink()
        } else {
            // Marquer qu'une lecture est en attente
            pendingPlay = true
        }
    }

    /// Pauses video playback and stops frame capture.
    func pause() {
        isPlaying = false
        player?.pause()
        stopDisplayLink()

        // Capture the current frame to display while paused (not for HLS)
        if !isHLSStream, let output = videoOutput, let item = player?.currentItem {
            let currentTime = item.currentTime()
            if output.hasNewPixelBuffer(forItemTime: currentTime),
               let pixelBuffer = output.copyPixelBuffer(
                   forItemTime: currentTime, itemTimeForDisplay: nil)
            {
                updateLatestFrameData(from: pixelBuffer)
            }
        }
    }

    /// Sets the volume level (0.0 to 1.0)
    func setVolume(level: Float) {
        volume = max(0.0, min(1.0, level))  // Clamp between 0.0 and 1.0
        // Use the standard method
        player?.volume = volume
    }

    /// Returns a pointer to the shared frame buffer. The caller should not free this pointer.
    func getLatestFramePointer() -> UnsafeMutablePointer<UInt32>? {
        return frameBuffer
    }

    /// Returns the width of the video frame in pixels
    func getFrameWidth() -> Int { return frameWidth }

    /// Returns the height of the video frame in pixels
    func getFrameHeight() -> Int { return frameHeight }

    /// Returns the duration of the video in seconds.
    func getDuration() -> Double {
        guard let item = player?.currentItem else { return 0 }

        // For live HLS streams, duration might be indefinite
        if isHLSStream && item.duration.isIndefinite {
            return -1  // Indicate live stream
        }

        // Use item.duration which is not deprecated
        return CMTimeGetSeconds(item.duration)
    }

    /// Seeks to the specified time (in seconds).
    func seekTo(time: Double) {
        guard let player = player else { return }
        let newTime = CMTime(seconds: time, preferredTimescale: 600)

        // For HLS, use tolerance for more efficient seeking
        if isHLSStream {
            let tolerance = CMTime(seconds: 1.0, preferredTimescale: 600)
            player.seek(to: newTime, toleranceBefore: tolerance, toleranceAfter: tolerance)
        } else {
            player.seek(to: newTime)

            // Update frame at the new position if paused
            if !isPlaying, let output = videoOutput {
                if output.hasNewPixelBuffer(forItemTime: newTime),
                   let pixelBuffer = output.copyPixelBuffer(
                       forItemTime: newTime, itemTimeForDisplay: nil)
                {
                    updateLatestFrameData(from: pixelBuffer)
                }
            }
        }
    }

    // MARK: - Callback Registration

    func registerStatusCallback(_ ctx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?, Int32) -> Void)?) {
        callbackContext = ctx
        statusCallback = callback
        attachCallbackObservers()
    }

    func unregisterStatusCallback() {
        timeControlStatusObserver?.invalidate()
        timeControlStatusObserver = nil
        statusCallback = nil
    }

    func registerTimeCallback(_ ctx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?, Double, Double) -> Void)?, _ interval: Double) {
        callbackContext = ctx
        timeCallback = callback
        timeObserverInterval = interval
        // Remove old observer if re-registering
        if let observer = periodicTimeObserver {
            player?.removeTimeObserver(observer)
            periodicTimeObserver = nil
        }
        attachCallbackObservers()
    }

    func unregisterTimeCallback() {
        if let observer = periodicTimeObserver {
            player?.removeTimeObserver(observer)
            periodicTimeObserver = nil
        }
        timeCallback = nil
    }

    func registerFrameCallback(_ ctx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?) -> Void)?) {
        callbackContext = ctx
        frameCallback = callback
    }

    func unregisterFrameCallback() {
        frameCallback = nil
    }

    func registerEndOfPlaybackCallback(_ ctx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?) -> Void)?) {
        callbackContext = ctx
        endOfPlaybackCallback = callback
        // Remove old observer if re-registering
        if let observer = endOfPlaybackObserver {
            NotificationCenter.default.removeObserver(observer)
            endOfPlaybackObserver = nil
        }
        attachCallbackObservers()
    }

    func unregisterEndOfPlaybackCallback() {
        if let observer = endOfPlaybackObserver {
            NotificationCenter.default.removeObserver(observer)
            endOfPlaybackObserver = nil
        }
        endOfPlaybackCallback = nil
    }

    /// Attaches observers for any callbacks that were registered before the player existed.
    /// Called from setupVideoOutputAndPlayer once the AVPlayer instance is ready.
    private func attachCallbackObservers() {
        guard let player = player else { return }

        // Status: KVO on timeControlStatus
        if statusCallback != nil && timeControlStatusObserver == nil {
            timeControlStatusObserver = player.observe(\.timeControlStatus, options: [.new]) { [weak self] player, _ in
                self?.handleTimeControlStatus(player.timeControlStatus)
            }
        }

        // Time: periodic time observer
        if timeCallback != nil && periodicTimeObserver == nil {
            let cmInterval = CMTime(seconds: timeObserverInterval, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
            periodicTimeObserver = player.addPeriodicTimeObserver(forInterval: cmInterval, queue: .main) { [weak self] time in
                guard let self = self else { return }
                let currentTime = CMTimeGetSeconds(time)
                let duration = CMTimeGetSeconds(self.player?.currentItem?.duration ?? CMTime.zero)
                self.timeCallback?(self.callbackContext, currentTime, duration.isNaN ? 0.0 : duration)
            }
        }

        // End of playback: notification observer
        if endOfPlaybackCallback != nil && endOfPlaybackObserver == nil {
            endOfPlaybackObserver = NotificationCenter.default.addObserver(
                forName: .AVPlayerItemDidPlayToEndTime,
                object: player.currentItem,
                queue: .main
            ) { [weak self] _ in
                guard let self = self else { return }
                self.endOfPlaybackCallback?(self.callbackContext)
            }
        }
    }

    private func unregisterAllCallbacks() {
        unregisterStatusCallback()
        unregisterTimeCallback()
        unregisterFrameCallback()
        unregisterEndOfPlaybackCallback()
        callbackContext = nil
    }

    private func cleanupObservers() {
        playerItemObserver?.invalidate()
        playerObserver?.invalidate()
        bufferEmptyObserver?.invalidate()
        bufferLikelyToKeepUpObserver?.invalidate()
        bufferFullObserver?.invalidate()
        NotificationCenter.default.removeObserver(self)
    }

    /// Disposes of the video player and releases resources.
    func dispose() {
        pause()
        unregisterAllCallbacks()
        cleanupObservers()
        player = nil
        videoOutput = nil
        if let buffer = frameBuffer {
            buffer.deallocate()
            frameBuffer = nil
            bufferCapacity = 0
        }
    }
}

/// MARK: - C Exported Functions for JNA

@_cdecl("createVideoPlayer")
public func createVideoPlayer() -> UnsafeMutableRawPointer? {
    let player = SharedVideoPlayer()
    return Unmanaged.passRetained(player).toOpaque()
}

@_cdecl("openUri")
public func openUri(_ context: UnsafeMutableRawPointer?, _ uri: UnsafePointer<CChar>?) {
    guard let context = context,
          let uriCStr = uri,
          let swiftUri = String(validatingUTF8: uriCStr)
    else {
        print("Invalid parameters for openUri")
        return
    }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    // Use a background queue for heavy operations to avoid blocking the main thread
    DispatchQueue.global(qos: .userInitiated).async {
        player.openUri(swiftUri)
    }
}

@_cdecl("playVideo")
public func playVideo(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.play()
    }
}

@_cdecl("pauseVideo")
public func pauseVideo(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.pause()
    }
}

@_cdecl("setVolume")
public func setVolume(_ context: UnsafeMutableRawPointer?, _ volume: Float) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.setVolume(level: volume)
    }
}

@_cdecl("getLatestFrame")
public func getLatestFrame(_ context: UnsafeMutableRawPointer?) -> UnsafeMutableRawPointer? {
    guard let context = context else { return nil }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    if let ptr = player.getLatestFramePointer() {
        return UnsafeMutableRawPointer(ptr)
    }
    return nil
}

@_cdecl("getFrameWidth")
public func getFrameWidth(_ context: UnsafeMutableRawPointer?) -> Int32 {
    guard let context = context else { return 0 }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    return Int32(player.getFrameWidth())
}

@_cdecl("getFrameHeight")
public func getFrameHeight(_ context: UnsafeMutableRawPointer?) -> Int32 {
    guard let context = context else { return 0 }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    return Int32(player.getFrameHeight())
}

@_cdecl("getVideoDuration")
public func getVideoDuration(_ context: UnsafeMutableRawPointer?) -> Double {
    guard let context = context else { return 0 }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    return player.getDuration()
}

@_cdecl("seekTo")
public func seekTo(_ context: UnsafeMutableRawPointer?, _ time: Double) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.seekTo(time: time)
    }
}

@_cdecl("disposeVideoPlayer")
public func disposeVideoPlayer(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeRetainedValue()
    DispatchQueue.main.async {
        player.dispose()
    }
}

// MARK: - Callback Registration C Exports

@_cdecl("registerStatusCallback")
public func registerStatusCallback(_ context: UnsafeMutableRawPointer?, _ callbackCtx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?, Int32) -> Void)?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    player.registerStatusCallback(callbackCtx, callback)
}

@_cdecl("unregisterStatusCallback")
public func unregisterStatusCallback(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    player.unregisterStatusCallback()
}

@_cdecl("registerTimeCallback")
public func registerTimeCallback(_ context: UnsafeMutableRawPointer?, _ callbackCtx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?, Double, Double) -> Void)?, _ interval: Double) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.registerTimeCallback(callbackCtx, callback, interval)
    }
}

@_cdecl("unregisterTimeCallback")
public func unregisterTimeCallback(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.unregisterTimeCallback()
    }
}

@_cdecl("registerFrameCallback")
public func registerFrameCallback(_ context: UnsafeMutableRawPointer?, _ callbackCtx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?) -> Void)?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    player.registerFrameCallback(callbackCtx, callback)
}

@_cdecl("unregisterFrameCallback")
public func unregisterFrameCallback(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    player.unregisterFrameCallback()
}

@_cdecl("registerEndOfPlaybackCallback")
public func registerEndOfPlaybackCallback(_ context: UnsafeMutableRawPointer?, _ callbackCtx: UnsafeRawPointer?, _ callback: (@convention(c) (UnsafeRawPointer?) -> Void)?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.registerEndOfPlaybackCallback(callbackCtx, callback)
    }
}

@_cdecl("unregisterEndOfPlaybackCallback")
public func unregisterEndOfPlaybackCallback(_ context: UnsafeMutableRawPointer?) {
    guard let context = context else { return }
    let player = Unmanaged<SharedVideoPlayer>.fromOpaque(context).takeUnretainedValue()
    DispatchQueue.main.async {
        player.unregisterEndOfPlaybackCallback()
    }
}
