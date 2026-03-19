package com.tunjid.heron.media.video.linux

import org.freedesktop.gstreamer.Gst
import org.freedesktop.gstreamer.Version

/**
 * Wraps GStreamer initialisation so it only runs once per process.
 * [Gst.init] is not idempotent across all versions of gst1-java-core,
 * so we guard it ourselves.
 */
internal object GStreamer {
    @Volatile private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            // Version.BASELINE = GStreamer 1.8 minimum.
            // On Linux the system libraries are located automatically.
            Gst.init(Version.BASELINE, "KMPVideoPlayer")
            initialized = true
        }
    }
}
